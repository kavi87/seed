/**
 * Copyright (c) 2013-2015, The SeedStack authors <http://seedstack.org>
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package org.seedstack.seed.security.internal.fixtures.data;


import org.seedstack.seed.security.spi.data.DataObfuscationHandler;

/**
 *
 * 
 * @author epo.jemba@ext.mpsa.com
 *
 */
public class DummyObfuscation implements DataObfuscationHandler<String> {

	@Override
	public String obfuscate(String data) {
		return "obfuscated!!";
	}

}
