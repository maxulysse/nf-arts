/*
 * Copyright 2026, Maxime U. Garcia
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

package nfarts

import groovy.transform.CompileStatic

/**
 * One task of the run, reduced to what the styles draw.
 *
 * @param process process name
 * @param start start time, epoch milliseconds
 * @param duration wall time, milliseconds
 * @param memory peak RSS, bytes
 * @param status COMPLETED, FAILED or CACHED
 */
@CompileStatic
record TaskTile(String process, long start, long duration, long memory, String status) {}
