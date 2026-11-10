import java.util.ArrayList;
import java.util.List;

/**
 * Recursive-descent parser for the coursework language.
 *
 * Assumptions caused by the missing non-terminals/tree diagrams in the
 * pasted grammar: at least one function, one parameter per function,
 * one semicolon-terminated statement per block, and expressions made
 * from arithmetic plus an optional equality comparison.
 *
 * No new SyntaxNode or SyntaxError constants are defined.
 */
public class Syntax {
    LexToken tokens[];

    private final ArrayList<SyntaxError> errors = new ArrayList<>();
    private int position = 0;

    Syntax(LexToken tokens[]) {
        this.tokens = tokens == null ? new LexToken[0] : tokens;
    }

    SyntaxNode parse() {
        position = 0;
        errors.clear();

        ArrayList<SyntaxNode> functions = new ArrayList<>();
        while (!atEnd()) {
            int start = position;
            try {
                functions.add(parseFunction());
            } catch (ParseFailure failure) {
                recoverFunction();
                if (position == start && !atEnd()) {
                    position++; // guarantee progress on malformed input
                }
            }
        }

        if (functions.isEmpty() && errors.isEmpty()) {
            error("Expected at least one function");
        }

        SyntaxNode functionList = node(SyntaxNode.FUNCTION_LIST,
                firstPosition(functions), null, functions);
        SyntaxNode program = node(SyntaxNode.PROGRAM,
                firstPosition(functions), null, functionList);

        // The contract does not specify whether to return a partial AST on errors.
        // Returning null makes failed parsing unambiguous; getErrors() still works.
        return errors.isEmpty() ? program : null;
    }

    SyntaxError[] getErrors() {
        return errors.toArray(new SyntaxError[0]);
    }

    private SyntaxNode parseFunction() {
        LexToken start = peek();
        SyntaxNode type = leaf(SyntaxNode.TYPE_NAME, consumeTypeName());
        SyntaxNode name = leaf(SyntaxNode.IDENTIFIER, consumeIdentifier());
        expect("(");
        SyntaxNode parameters = parseParameterList();
        expect(")");
        SyntaxNode statements = parseStatementList("endf");
        expect("endf");
        return node(SyntaxNode.FUNCTION, start, null, type, name, parameters, statements);
    }

    private SyntaxNode parseParameterList() {
        LexToken start = peek();
        ArrayList<SyntaxNode> parameters = new ArrayList<>();
        // No epsilon alternative is visible in the pasted parameter-list grammar.
        parameters.add(parseParameter());
        while (match(",")) {
            parameters.add(parseParameter());
        }
        return node(SyntaxNode.PARAMETER_LIST, start, null, parameters);
    }

    private SyntaxNode parseParameter() {
        LexToken start = peek();
        SyntaxNode type = leaf(SyntaxNode.TYPE_NAME, consumeTypeName());
        SyntaxNode identifier = leaf(SyntaxNode.IDENTIFIER, consumeIdentifier());
        return node(SyntaxNode.PARAMETER, start, null, type, identifier);
    }

    private SyntaxNode parseStatementList(String... terminators) {
        LexToken start = peek();
        ArrayList<SyntaxNode> statements = new ArrayList<>();
        boolean attemptedStatement = false;
        while (!atEnd() && !isBlockStop(terminators)) {
            attemptedStatement = true;
            int before = position;
            try {
                SyntaxNode statement = parseStatement();
                expect(";");
                statements.add(statement);
            } catch (ParseFailure failure) {
                recoverStatement();
                if (position == before && !atEnd() && !isBlockStop(terminators)) {
                    position++;
                }
            }
        }
        if (!attemptedStatement) {
            error("Expected at least one statement");
        }
        return node(SyntaxNode.STATEMENT_LIST, start, null, statements);
    }

    private SyntaxNode parseStatement() {
        LexToken start = peek();
        SyntaxNode content;
        if (typeIs(LexToken.TYPE_NAME)) {
            content = parseDeclaration();
        } else if (typeIs(LexToken.IDENTIFIER)) {
            content = parseAssignment();
        } else if (check("if")) {
            content = parseIf();
        } else if (check("while")) {
            content = parseWhile();
        } else if (check("return")) {
            content = parseReturn();
        } else {
            throw error("Expected declaration, assignment, if, while, or return");
        }
        return node(SyntaxNode.STATEMENT, start, null, content);
    }

    private SyntaxNode parseDeclaration() {
        LexToken start = peek();
        SyntaxNode type = leaf(SyntaxNode.TYPE_NAME, consumeTypeName());
        // Grammar: declare ::= typeName identifier | typeName assign
        // An initialised declaration therefore contains an ASSIGN subtree.
        if (typeIs(LexToken.IDENTIFIER) &&
                position + 1 < tokens.length &&
                "=".equals(tokens[position + 1].token)) {
            return node(SyntaxNode.DECLARE, start, null, type, parseAssignment());
        }
        SyntaxNode name = leaf(SyntaxNode.IDENTIFIER, consumeIdentifier());
        return node(SyntaxNode.DECLARE, start, null, type, name);
    }

    private SyntaxNode parseAssignment() {
        LexToken start = peek();
        SyntaxNode identifier = leaf(SyntaxNode.IDENTIFIER, consumeIdentifier());
        expect("=");
        // Grammar explicitly allows string literals on assignment RHS.
        SyntaxNode value = typeIs(LexToken.LITERAL_STRING)
                ? leaf(SyntaxNode.STRING, advance()) : parseExpression();
        return node(SyntaxNode.ASSIGN, start, null, identifier, value);
    }

    private SyntaxNode parseIf() {
        LexToken start = expect("if");
        expect("(");
        SyntaxNode condition = parseExpression();
        expect(")");
        SyntaxNode trueBranch = parseStatementList("else", "endif");
        if (match("else")) {
            SyntaxNode falseBranch = parseStatementList("endif");
            expect("endif");
            return node(SyntaxNode.IF, start, null, condition, trueBranch, falseBranch);
        }
        expect("endif");
        return node(SyntaxNode.IF, start, null, condition, trueBranch);
    }

    private SyntaxNode parseWhile() {
        LexToken start = expect("while");
        expect("(");
        SyntaxNode condition = parseExpression();
        expect(")");
        SyntaxNode body = parseStatementList("endw");
        expect("endw");
        return node(SyntaxNode.WHILE, start, null, condition, body);
    }

    private SyntaxNode parseReturn() {
        LexToken start = expect("return");
        SyntaxNode value = parseExpression();
        return node(SyntaxNode.RETURN, start, null, value);
    }

    private SyntaxNode parseExpression() {
        LexToken start = peek();
        SyntaxNode left = parseMath();
        if (match("==")) {
            LexToken equality = previous();
            SyntaxNode right = parseMath();
            SyntaxNode comparison = node(SyntaxNode.COMPARISON,
                    equality, "==", left, right);
            return node(SyntaxNode.EXPRESSION, start, null, comparison);
        }
        return node(SyntaxNode.EXPRESSION, start, null, left);
    }

    // Eliminates left recursion by parsing operators in loops.
    private SyntaxNode parseMath() {
        LexToken start = peek();
        SyntaxNode left = parseTerm();
        while (check("+") || check("-")) {
            LexToken operator = advance();
            SyntaxNode right = parseTerm();
            int type = operator.token.equals("+")
                    ? SyntaxNode.MATH_ADD : SyntaxNode.MATH_SUBTRACT;
            left = node(type, operator, operator.token, left, right);
        }
        return node(SyntaxNode.MATH, start, null, left);
    }

    private SyntaxNode parseTerm() {
        LexToken start = peek();
        SyntaxNode left = parseFactor();
        while (check("*") || check("/")) {
            LexToken operator = advance();
            SyntaxNode right = parseFactor();
            int type = operator.token.equals("*")
                    ? SyntaxNode.TERM_MULTIPLY : SyntaxNode.TERM_DIVIDE;
            left = node(type, operator, operator.token, left, right);
        }
        return node(SyntaxNode.TERM, start, null, left);
    }

    private SyntaxNode parseFactor() {
        LexToken start = peek();
        if (typeIs(LexToken.LITERAL_NUMBER)) {
            return node(SyntaxNode.FACTOR, start, null,
                    leaf(SyntaxNode.NUMBER, advance()));
        }
        if (typeIs(LexToken.IDENTIFIER)) {
            return node(SyntaxNode.FACTOR, start, null,
                    leaf(SyntaxNode.IDENTIFIER, advance()));
        }
        if (match("(")) {
            SyntaxNode inner = parseMath();
            expect(")");
            return node(SyntaxNode.FACTOR, start, null, inner);
        }
        throw error("Expected a number, identifier or parenthesised expression");
    }

    private LexToken consumeIdentifier() {
        if (!typeIs(LexToken.IDENTIFIER)) {
            throw error("Expected an identifier");
        }
        return advance();
    }

    private LexToken consumeTypeName() {
        if (!typeIs(LexToken.TYPE_NAME)) {
            throw error("Expected a type name (int, float, string, bool)");
        }
        return advance();
    }

    private LexToken expect(String text) {
        if (!check(text)) {
            throw error("Expected '" + text + "'");
        }
        return advance();
    }

    private boolean match(String text) {
        if (check(text)) {
            advance();
            return true;
        }
        return false;
    }

    private boolean isBlockStop(String[] closers) {
        // Also stop for unexpected block endings, so the parent can report
        // the missing expected closer rather than swallowing everything.
        return checkAny("endf", "else", "endif", "endw") || checkAny(closers);
    }

    private boolean checkAny(String... words) {
        for (String word : words) {
            if (check(word)) return true;
        }
        return false;
    }

    private boolean check(String word) {
        return !atEnd() && word.equals(tokens[position].token);
    }

    private boolean typeIs(int type) {
        return !atEnd() && tokens[position].type == type;
    }

    private boolean atEnd() {
        return position >= tokens.length;
    }

    private LexToken peek() {
        return atEnd() ? null : tokens[position];
    }

    private LexToken previous() {
        return position == 0 ? null : tokens[position - 1];
    }

    private LexToken advance() {
        return tokens[position++];
    }

    private ParseFailure error(String message) {
        LexToken at = peek();
        if (at == null) {
            LexToken last = tokens.length == 0 ? null : tokens[tokens.length - 1];
            int line = last == null ? 1 : last.lineNumber;
            int col = last == null ? 1 : last.characterOffset + last.token.length();
            errors.add(new SyntaxError(SyntaxError.UNKNOWN_SYNTAX,
                    line, col, message + " (at end of input)"));
        } else {
            errors.add(new SyntaxError(SyntaxError.UNKNOWN_SYNTAX,
                    at.lineNumber, at.characterOffset,
                    message + "; found '" + at.token + "'"));
        }
        return new ParseFailure();
    }

    private void recoverStatement() {
        while (!atEnd() && !check(";") &&
                !checkAny("else", "endif", "endw", "endf")) {
            advance();
        }
        match(";");
    }

    private void recoverFunction() {
        while (!atEnd()) {
            if (check("endf")) {
                advance();
                return;
            }
            if (typeIs(LexToken.TYPE_NAME) && position + 2 < tokens.length &&
                    tokens[position + 1].type == LexToken.IDENTIFIER &&
                    "(".equals(tokens[position + 2].token)) {
                return;
            }
            advance();
        }
    }

    private static SyntaxNode leaf(int type, LexToken token) {
        SyntaxNode result = new SyntaxNode(type,
                token.lineNumber, token.characterOffset, token.token);
        result.children = new SyntaxNode[0];
        return result;
    }

    private static SyntaxNode node(int type, LexToken location,
                                   String token, SyntaxNode... children) {
        int line = location == null ? 1 : location.lineNumber;
        int column = location == null ? 1 : location.characterOffset;
        SyntaxNode result = new SyntaxNode(type, line, column, token);
        result.children = children;
        return result;
    }

    private static SyntaxNode node(int type, LexToken location,
                                   String token, List<SyntaxNode> children) {
        return node(type, location, token, children.toArray(new SyntaxNode[0]));
    }

    private LexToken firstPosition(List<SyntaxNode> nodes) {
        if (!nodes.isEmpty()) {
            SyntaxNode first = nodes.get(0);
            return new LexToken(0, first.lineNumber, first.characterOffset, "");
        }
        return tokens.length == 0 ? null : tokens[0];
    }

    private static class ParseFailure extends RuntimeException {
        private static final long serialVersionUID = 1L;
    }
}
