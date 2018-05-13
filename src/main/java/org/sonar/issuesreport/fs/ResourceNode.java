/*
 * Sonar :: Issues Report :: Plugin
 * Copyright (C) 2013 SonarSource
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.issuesreport.fs;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.resources.Scopes;

import java.io.File;
import java.nio.charset.Charset;

import javax.annotation.CheckForNull;

/**
 * Simplified version of {@link org.sonar.api.resources.Resource}
 *
 */
public class ResourceNode {

  private final String key;
  private final String longName;
  private final File path;
  private final Charset encoding;
  private final String scope;

  public ResourceNode(InputFile inputFile) {
    this.path = inputFile.file();
    this.encoding = inputFile.charset();
    this.key = inputFile.key();
    this.longName = inputFile.toString();
    this.scope = inputFile.isFile() ? Scopes.FILE : Scopes.DIRECTORY;
  }

  public String getKey() {
    return key;
  }

  public String getScope() {
    return scope;
  }

  public String getName() {
    return longName;
  }

  @CheckForNull
  public File getPath() {
    return path;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (obj.getClass() != getClass()) {
      return false;
    }
    ResourceNode rhs = (ResourceNode) obj;
    return new EqualsBuilder().append(this.key, rhs.key).isEquals();
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  public Charset getEncoding() {
    return encoding;
  }

}
