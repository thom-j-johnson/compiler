            char c = stream.charAt(index);

            if (isWhiteSpace(c)) {
                advance();
            } else if (c == '"') {
                scanString();
            } else if (isDigit(c)) {
                scanNumber();
            } else if (isSyntaxChar(c)) {
                scanSyntax();
            } else {
                scanIdentifierOrReservedWord();
            }
        }
    }

    private void scanSyntax() {
        int start = index;
        int startLine = line;
        int startColumn = column;

        if (stream.charAt(index) == '=' &&
            index + 1 < stream.length() && stream.charAt(index + 1) == '=') {
            advance();
            advance();
        } else {
            advance();
        }

        addToken(LexToken.SYNTAX_TOKEN, start, startLine, startColumn);
    }

    private void scanIdentifierOrReservedWord() {
        int start = index;
        int startLine = line;
        int startColumn = column;

        while (index < stream.length()) {
            char c = stream.charAt(index);
            if (isWhiteSpace(c) || isSyntaxChar(c) || c == '"') {
                break;
            }
            advance();
        }

        String word = stream.substring(start, index);
        int type;
        if (isKeyword(word)) {
            type = LexToken.KEYWORD;
        } else if (isTyping(word)) {
            type = LexToken.TYPE_NAME;
        } else {
            type = LexToken.IDENTIFIER;
        }
        addToken(type, start, startLine, startColumn);
    }

    private void scanNumber() {
        int start = index;
        int startLine = line;
        int startColumn = column;

        while (index < stream.length() && isDigit(stream.charAt(index))) {
            advance();
        }

        // Consume a decimal point only when followed by a digit.
        if (index + 1 < stream.length() &&
            stream.charAt(index) == '.' &&
            isDigit(stream.charAt(index + 1))) {
            advance();
            while (index < stream.length() && isDigit(stream.charAt(index))) {
                advance();
            }
        }

        // A digit-starting chunk containing other identifier characters
        // cannot be an identifier or a valid numeric literal.
        boolean malformed = false;
        while (index < stream.length()) {
            char c = stream.charAt(index);
            if (isWhiteSpace(c) || isSyntaxChar(c) || c == '"') {
                break;
            }
            malformed = true;
            advance();
        }

        if (malformed) {
            addError(LexError.UNKNOWN_NUMBER_FORMAT, startLine, startColumn,
                     "Invalid numeric literal");
            addToken(LexToken.UNKNOWN, start, startLine, startColumn);
        } else {
            addToken(LexToken.LITERAL_NUMBER, start, startLine, startColumn);
        }
    }

    private void scanString() {
        int start = index;
        int startLine = line;
        int startColumn = column;
        boolean closed = false;
        boolean invalidEscape = false;

        advance(); // opening quote
        while (index < stream.length()) {
            char c = stream.charAt(index);
            if (c == '\n' || c == '\r') {
                break; // strings cannot span multiple lines
            }
            if (c == '"') {
                advance();
                closed = true;
                break;
            }
            if (c == '\\') {
                advance();
                if (index >= stream.length() ||
                    stream.charAt(index) == '\r' || stream.charAt(index) == '\n') {
                    break;
                }
                char escaped = stream.charAt(index);
                if (escaped != 'r' && escaped != 'n' && escaped != 't' &&
                    escaped != '"' && escaped != '\\') {
                    invalidEscape = true;
                    addError(LexError.UNKNOWN_ESCAPE_CHARACTER, startLine, startColumn,
                             "Invalid escape sequence in string literal");
                }
                advance();
            } else {
                advance();
            }
        }

        if (!closed) {
            addError(LexError.UNKNOWN_STRING_FORMAT, startLine, startColumn,
                     "Unterminated string literal");
        }

        addToken(closed && !invalidEscape ? LexToken.LITERAL_STRING : LexToken.UNKNOWN,
                 start, startLine, startColumn);
    }
}
