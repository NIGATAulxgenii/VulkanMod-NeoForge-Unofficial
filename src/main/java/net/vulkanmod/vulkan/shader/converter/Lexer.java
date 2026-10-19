package net.vulkanmod.vulkan.shader.converter;

import java.util.ArrayList;
import java.util.List;

public class Lexer {
   private final String input;
   private int currentPosition;
   private char currentChar;
   private Lexer.State state;

   public Lexer(String input) {
      this.input = input;
      this.currentPosition = 0;
      this.currentChar = !input.isEmpty() ? input.charAt(0) : 0;
   }

   private void advance() {
      this.advance(1);
   }

   private void advance(int i) {
      for (int j = 0; j < i; j++) {
         this.currentPosition++;
         if (this.currentPosition >= this.input.length()) {
            this.currentChar = 0;
            break;
         }

         this.currentChar = this.input.charAt(this.currentPosition);
      }
   }

   private char peek() {
      int peekPosition = this.currentPosition + 1;
      return peekPosition < this.input.length() ? this.input.charAt(peekPosition) : '\u0000';
   }

   public List<Token> tokenize() {
      List<Token> tokens = new ArrayList<>();

      while (this.currentPosition < this.input.length()) {
         char currentChar = this.input.charAt(this.currentPosition);
         Token token = this.nextToken();
         if (token == null) {
            throw new RuntimeException("Unknown character: " + currentChar);
         }

         tokens.add(token);
      }

      tokens.add(new Token(Token.TokenType.EOF, null));
      return tokens;
   }

   public Token nextToken() {
      if (!this.checkEOF()) {
         return new Token(Token.TokenType.EOF, null);
      } else {
         if (this.currentChar == '/') {
            switch (this.peek()) {
               case '*':
                  return this.multiLineComment();
               case '/':
                  return this.lineComment();
            }
         }

         label72:
         switch (this.currentChar) {
            case '!':
               if (this.peek() == '=') {
                  this.advance(2);
                  return new Token(Token.TokenType.OPERATOR, "!=");
               }
               break;
            case '<':
               switch (this.peek()) {
                  case '<':
                     this.advance(2);
                     return new Token(Token.TokenType.OPERATOR, "<<");
                  case '=':
                     this.advance(2);
                     return new Token(Token.TokenType.OPERATOR, "<=");
                  default:
                     break label72;
               }
            case '=':
               if (this.peek() == '=') {
                  this.advance(2);
                  return new Token(Token.TokenType.OPERATOR, "==");
               }
               break;
            case '>':
               switch (this.peek()) {
                  case '=':
                     this.advance(2);
                     return new Token(Token.TokenType.OPERATOR, ">=");
                  case '>':
                     this.advance(2);
                     return new Token(Token.TokenType.OPERATOR, ">>");
               }
         }
         Token token = switch (this.currentChar) {
            case '!' -> new Token(Token.TokenType.OPERATOR, "!");
            case '"' -> this.string();
            case '#' -> new Token(Token.TokenType.PREPROCESSOR, "#");
            default -> null;
            case '%' -> new Token(Token.TokenType.OPERATOR, "%");
            case '&' -> new Token(Token.TokenType.OPERATOR, "&");
            case '(' -> new Token(Token.TokenType.LEFT_PARENTHESIS, "(");
            case ')' -> new Token(Token.TokenType.RIGHT_PARENTHESIS, ")");
            case '*' -> new Token(Token.TokenType.OPERATOR, "*");
            case '+' -> new Token(Token.TokenType.OPERATOR, "+");
            case ',' -> new Token(Token.TokenType.COMMA, ",");
            case '-' -> new Token(Token.TokenType.OPERATOR, "-");
            case '.' -> new Token(Token.TokenType.DOT, ".");
            case '/' -> new Token(Token.TokenType.OPERATOR, "/");
            case ':' -> new Token(Token.TokenType.COLON, ":");
            case ';' -> new Token(Token.TokenType.SEMICOLON, ";");
            case '<' -> new Token(Token.TokenType.OPERATOR, "<");
            case '=' -> new Token(Token.TokenType.OPERATOR, "=");
            case '>' -> new Token(Token.TokenType.OPERATOR, ">");
            case '?' -> new Token(Token.TokenType.OPERATOR, "?");
            case '[' -> new Token(Token.TokenType.OPERATOR, "[");
            case ']' -> new Token(Token.TokenType.OPERATOR, "]");
            case '^' -> new Token(Token.TokenType.OPERATOR, "^");
            case '{' -> new Token(Token.TokenType.LEFT_BRACE, "{");
            case '|' -> new Token(Token.TokenType.OPERATOR, "|");
            case '}' -> new Token(Token.TokenType.RIGHT_BRACE, "}");
         };
         if (token == null) {
            if (Character.isLetter(this.currentChar) || this.currentChar == '_') {
               return this.identifier();
            }

            if (Character.isDigit(this.currentChar)) {
               return this.literal();
            }

            if (Character.isWhitespace(this.currentChar)) {
               return this.spacing();
            }
         }

         if (token == null) {
            throw new IllegalStateException("Unrecognized char: " + this.currentChar);
         } else {
            this.advance();
            return token;
         }
      }
   }

   private Token lineComment() {
      StringBuilder sb = new StringBuilder();
      sb.append("//");
      this.advance(2);

      while (this.checkEOF() && this.currentChar != '\n') {
         sb.append(this.currentChar);
         this.advance();
      }

      sb.append(this.currentChar);
      this.advance();
      String value = sb.toString();
      return new Token(Token.TokenType.COMMENT, value);
   }

   private Token multiLineComment() {
      StringBuilder sb = new StringBuilder();
      sb.append("/*");
      this.advance(2);

      while (this.checkEOF() && this.currentChar != '*' && this.peek() != '/') {
         sb.append(this.currentChar);
         this.advance();
      }

      sb.append(this.currentChar);
      this.advance();
      sb.append(this.currentChar);
      this.advance();
      String value = sb.toString();
      return new Token(Token.TokenType.COMMENT, value);
   }

   private Token identifier() {
      StringBuilder sb = new StringBuilder();

      while (this.checkEOF() && Character.isJavaIdentifierPart(this.currentChar)) {
         sb.append(this.currentChar);
         this.advance();
      }

      String value = sb.toString();
      return new Token(Token.TokenType.IDENTIFIER, value);
   }

   private Token literal() {
      StringBuilder sb = new StringBuilder();

      while (Character.isDigit(this.currentChar)) {
         sb.append(this.currentChar);
         this.advance();
      }

      if (this.currentChar == '.') {
         sb.append(this.currentChar);
         this.advance();
      }

      while (Character.isDigit(this.currentChar)) {
         sb.append(this.currentChar);
         this.advance();
      }

      String value = sb.toString();
      return new Token(Token.TokenType.LITERAL, value);
   }

   private Token string() {
      StringBuilder sb = new StringBuilder();
      sb.append(this.currentChar);
      this.advance();

      while (this.checkEOF() && this.currentChar != '"') {
         sb.append(this.currentChar);
         this.advance();
      }

      sb.append(this.currentChar);
      this.advance();
      String value = sb.toString();
      return new Token(Token.TokenType.STRING, value);
   }

   private Token spacing() {
      StringBuilder sb = new StringBuilder();

      while (this.currentChar != 0 && Character.isWhitespace(this.currentChar)) {
         sb.append(this.currentChar);
         this.advance();
      }

      String value = sb.toString();
      return new Token(Token.TokenType.SPACING, value);
   }

   private boolean checkEOF() {
      return this.currentChar != 0;
   }

   static enum State {
      UNIFORM_BLOCK,
      CODE,
      DEFAULT;
   }
}
