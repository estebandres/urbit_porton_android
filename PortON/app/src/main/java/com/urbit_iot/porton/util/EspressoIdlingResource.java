/*
 * Copyright 2016, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.urbit_iot.porton.util;

import androidx.test.espresso.IdlingResource;
import androidx.test.espresso.idling.CountingIdlingResource;

/**
 * Contains a static reference to {@link IdlingResource}, only available in the 'mock' build type.
 */
public class EspressoIdlingResource {

    private static final String RESOURCE = "GLOBAL";

    private static final SimpleCountingIdlingResource DEFAULT_INSTANCE =
            new SimpleCountingIdlingResource(RESOURCE);

    public static void increment() {
        DEFAULT_INSTANCE.increment();
    }

    public static void decrement() {
        DEFAULT_INSTANCE.decrement();
    }

    public static IdlingResource getIdlingResource() {
        return DEFAULT_INSTANCE;
    }
}
