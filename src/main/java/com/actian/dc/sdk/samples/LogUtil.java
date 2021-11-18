/*
 *
 * Created on: 7/22/2021
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

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * @author wbunton
 */
public class LogUtil {
    static Logger getLogger(Class<?> clazz) {
        // Set the log level to Info and send logs to the console
        Logger logger = Logger.getLogger(clazz.getName());
        logger.setLevel(Level.INFO);
        Handler handler = new ConsoleHandler();
        handler.setFormatter(new SimpleFormatter());
        return logger;
    }
}
