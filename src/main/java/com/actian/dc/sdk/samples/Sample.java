/*
 *
 * Created on: 7/21/2021
 *
 * Copyright (c) 2021 by Actian Corp.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.actian.dc.sdk.samples;

/**
 * @author wbunton
 */
public abstract class Sample {
    static final String ACCOUNTS_SRC = "$(SRC)/Accounts.txt";

    /**
     * Run the sample/example.  A return of true indicates everything ran okay.  A return of false indicates
     * something went wrong while running the sample.
     *
     * @return true if ran correctly, false if something went wrong
     */
    abstract protected boolean run();

    static String suppliedArtifactPath(String name) {
        return SamplesRunner.artifactsPath + "/" + name;
    }

    static String createdArtifactPath(String name) {
        return SamplesRunner.directoryFor("ARTIFACTS") + "/" + name;
    }

    @SuppressWarnings("MalformedFormatString")
    String escapeString(String src) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < src.length(); i++) {
            switch (src.charAt(i)) {
                case '\n':
                    sb.append("\\n");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                case '\f':
                    sb.append("\\f");
                default:
                    if (src.charAt(i) < 0x20 || src.charAt(i) == 0x7f)
                        sb.append(String.format("\\u%04x", src.charAt(i)));
                    else
                        sb.append(src.charAt(i));
            }
        }
        return sb.toString();
    }
}
