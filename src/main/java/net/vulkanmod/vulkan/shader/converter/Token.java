package net.vulkanmod.vulkan.shader.converter;

public class Token {
   public final Token.TokenType type;
   public String value;

   public Token(Token.TokenType type, String value) {
      this.type = type;
      this.value = value;
   }

   @Override
   public String toString() {
      return "Token{type=" + this.type + ", value='" + this.value + "'}";
   }

   public static enum TokenType {
      PREPROCESSOR,
      KEYWORD,
      IDENTIFIER,
      LITERAL,
      OPERATOR,
      PUNCTUATION,
      STRING,
      SPACING,
      COMMENT,
      LEFT_BRACE,
      RIGHT_BRACE,
      LEFT_PARENTHESIS,
      RIGHT_PARENTHESIS,
      COLON,
      SEMICOLON,
      DOT,
      COMMA,
      TYPE,
      LAYOUT,
      EOF;
   }
}
