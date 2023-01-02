/*
 *  Thaumcraft Research Patcher
 *  Copyright (c) 2023 TheCodex6824.
 *
 *  This file is part of Thaumcraft Research Patcher.
 *
 *  Thaumcraft Research Patcher is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Thaumcraft Research Patcher is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with Thaumcraft Research Patcher.  If not, see <https://www.gnu.org/licenses/>.
 */

package thecodex6824.tcresearchpatcher.json;

/**
 * Exception class for instances where the provided JSON does match the expected format.
 * @author TheCodex6824
 */
public class JsonSchemaException extends RuntimeException {

    private static final long serialVersionUID = -202697203598380455L;

    public JsonSchemaException(String cause) {
        super(cause);
    }
    
    public JsonSchemaException(Throwable cause) {
        super(cause);
    }
    
    public JsonSchemaException(String message, Throwable cause) {
        super(message, cause);
    }
    
}
