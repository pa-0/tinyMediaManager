/*
 * Copyright 2012 - 2024 Manuel Laggner
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tinymediamanager.core;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

public class TmmMultilineRecursiveToStringStyle extends org.apache.commons.lang3.builder.RecursiveToStringStyle {
  private static final int INDENT = 4;

  /** Current indenting. */
  private int              spaces = 4;

  public TmmMultilineRecursiveToStringStyle() {
    resetIndent();

    this.setUseShortClassName(true);
    this.setUseIdentityHashCode(false);
  }

  /**
   * Resets the fields responsible for the line breaks and indenting. Must be invoked after changing the {@link #spaces} value.
   */
  private void resetIndent() {
    setArrayStart("[" + System.lineSeparator() + spacer(spaces));
    setArraySeparator("," + System.lineSeparator() + spacer(spaces));
    setArrayEnd(System.lineSeparator() + spacer(spaces - INDENT) + "]");

    setContentStart(" {" + System.lineSeparator() + spacer(spaces));
    setFieldSeparator("," + System.lineSeparator() + spacer(spaces));
    setContentEnd(System.lineSeparator() + spacer(spaces - INDENT) + "}");
  }

  /**
   * Creates a StringBuilder responsible for the indenting.
   *
   * @param spaces
   *          how far to indent
   * @return a StringBuilder with {spaces} leading space characters.
   */
  private StringBuilder spacer(final int spaces) {
    final StringBuilder sb = new StringBuilder();
    for (int i = 0; i < spaces; i++) {
      sb.append(" ");
    }
    return sb;
  }

  @Override
  public void appendDetail(final StringBuffer buffer, final String fieldName, final Object value) {
    if (!value.getClass().isEnum() && !ClassUtils.isPrimitiveWrapper(value.getClass()) && !String.class.equals(value.getClass())
        && accept(value.getClass())) {
      // dig deeper
      spaces += INDENT;
      resetIndent();
      buffer.append(new GetterToStringBuilder(value));
      spaces -= INDENT;
      resetIndent();
    }
    else {
      buffer.append(value);
    }
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final Object[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void reflectionAppendArrayDetail(final StringBuffer buffer, final String fieldName, final Object array) {
    if (Array.getLength(array) == 0) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.reflectionAppendArrayDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final long[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final int[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final short[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final byte[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final char[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final double[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final float[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final boolean[] array) {
    if (ArrayUtils.isEmpty(array)) {
      buffer.append("[]");
      return;
    }

    spaces += INDENT;
    resetIndent();
    super.appendDetail(buffer, fieldName, array);
    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final Collection<?> coll) {
    appendDetail(buffer, fieldName, coll.toArray());
  }

  @Override
  protected void appendDetail(final StringBuffer buffer, final String fieldName, final Map<?, ?> map) {
    if (map.isEmpty()) {
      buffer.append("{}");
      return;
    }

    spaces += INDENT;
    resetIndent();

    buffer.append(getContentStart());

    for (var entry : map.entrySet()) {
      buffer.append(entry.getKey());
      buffer.append("=");
      buffer.append(entry.getValue());
    }

    buffer.append(getContentEnd());

    spaces -= INDENT;
    resetIndent();
  }

  @Override
  protected boolean accept(final Class<?> clazz) {
    if (clazz.getName().startsWith("sun.nio.fs.")) {
      return false;
    }

    return true;
  }

  private final class GetterToStringBuilder extends ReflectionToStringBuilder {

    public GetterToStringBuilder(Object object) {
      super(object, TmmMultilineRecursiveToStringStyle.this);
    }

    @Override
    protected void appendFieldsIn(final Class<?> clazz) {
      if (clazz.isArray()) {
        this.reflectionAppendArray(this.getObject());
        return;
      }

      try {
        PropertyDescriptor[] pds = Introspector.getBeanInfo(clazz).getPropertyDescriptors();
        for (PropertyDescriptor descriptor : pds) {

          if ("class".equals(descriptor.getDisplayName())) {
            continue;
          }

          if ("declaringClass".equals(descriptor.getDisplayName())) {
            continue;
          }

          if (descriptor.getReadMethod() != null) {
            this.append(descriptor.getName(), descriptor.getReadMethod().invoke(getObject()));
          }
        }
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
  }
}
