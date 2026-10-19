package net.vulkanmod.vulkan.shader.converter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UniformBlock implements GLSLParser.Node {
   int binding;
   String name;
   String alias;
   List<UniformBlock.Field> fields = new ArrayList<>();

   public UniformBlock(String name) {
      this.name = name;
   }

   public void addField(UniformBlock.Field field) {
      this.fields.add(field);
   }

   public void setBinding(int binding) {
      this.binding = binding;
   }

   public void setAlias(String alias) {
      this.alias = alias;
   }

   @Override
   public String getStringValue() {
      StringBuilder sb = new StringBuilder();
      sb.append("layout(binding = %d) uniform %s {\n".formatted(this.binding, this.name));
      int i = 0;

      for (int fieldsSize = this.fields.size(); i < fieldsSize; i++) {
         UniformBlock.Field field = this.fields.get(i);
         sb.append("\t%s %s;".formatted(field.type, field.name));
         if (i < fieldsSize - 1) {
            sb.append("\n");
         }
      }

      sb.append("\n}");
      if (this.alias != null) {
         sb.append(" %s ".formatted(this.alias));
      }

      sb.append(";\n");
      return sb.toString();
   }

   public static class Field {
      final String type;
      final String name;

      public Field(String type, String name) {
         this.type = type;
         this.name = name;
      }

      @Override
      public boolean equals(Object o) {
         if (o != null && this.getClass() == o.getClass()) {
            UniformBlock.Field field = (UniformBlock.Field)o;
            return Objects.equals(this.type, field.type) && Objects.equals(this.name, field.name);
         } else {
            return false;
         }
      }

      @Override
      public int hashCode() {
         int result = Objects.hashCode(this.type);
         return 31 * result + Objects.hashCode(this.name);
      }
   }
}
