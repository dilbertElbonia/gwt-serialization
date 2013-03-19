/*
 * Copyright 2013 monkeyboy
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

/**
 * Ne znam zašto su BlacklistTypeFilter stavili package private pa se na ovaj način zaobilazi taj problem.
 * Sada se može koristiti izvana.
 * <p/>
 * User: stole
 */
public class BlacklistFilter extends BlacklistTypeFilter {

    public BlacklistFilter(final TreeLogger logger, final PropertyOracle propertyOracle) throws UnableToCompleteException {
        super(logger, propertyOracle);
    }
}
