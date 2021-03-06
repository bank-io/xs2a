/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.validator.common;

import org.bouncycastle.asn1.DERUTF8String;

public class RoleOfPspName extends DERUTF8String {
	public static final RoleOfPspName PSP_AS = new RoleOfPspName("PSP_AS");
	public static final RoleOfPspName PSP_PI = new RoleOfPspName("PSP_PI");
	public static final RoleOfPspName PSP_AI = new RoleOfPspName("PSP_AI");
	public static final RoleOfPspName PSP_IC = new RoleOfPspName("PSP_IC");

	private RoleOfPspName(String string) {
		super(string);
	}
}
