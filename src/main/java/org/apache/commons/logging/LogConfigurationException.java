/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.commons.logging;

import java.util.Objects;

/**
 * An exception that is thrown only if a suitable {@code LogFactory} or {@code Log} instance cannot be created by the corresponding factory methods.
 */
public class LogConfigurationException extends RuntimeException {

    /** Serializable version identifier. */
    private static final long serialVersionUID = 8486587136871052495L;

    /**
     * The underlying cause of this exception.
     *
     * @deprecated Use {@link #getCause()}.
     */
    @Deprecated
    protected Throwable cause;

    /**
     * Constructs a new exception with {@code null} as its detail message.
     */
    public LogConfigurationException() {
    }

    /**
     * Constructs a new exception with the specified detail message.
     *
     * @param message The detail message
     */
    public LogConfigurationException(final String message) {
        super(message);
    }

    /**
     * Constructs a new exception with the specified detail message and cause.
     *
     * @param message The detail message
     * @param cause   The underlying cause
     */
    public LogConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
        this.cause = cause;
    }

    /**
     * Constructs a new exception with the specified cause and a derived detail message.
     *
     * @param cause The underlying cause
     */
    public LogConfigurationException(final Throwable cause) {
        this(Objects.toString(cause, null), cause);
    }

}
