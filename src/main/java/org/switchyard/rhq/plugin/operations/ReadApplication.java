/*
 * Copyright 2013 Red Hat Inc. and/or its affiliates and other contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.switchyard.rhq.plugin.operations;

import org.rhq.modules.plugins.jbossas7.json.Operation;
import static org.switchyard.rhq.plugin.SwitchYardConstants.ADDRESS_SWITCHYARD;
import static org.switchyard.rhq.plugin.SwitchYardConstants.DMR_READ_APPLICATION;
import static org.switchyard.rhq.plugin.SwitchYardConstants.PARAM_NAME;

/**
 * Read Application operation
 */
public class ReadApplication extends Operation {
    public ReadApplication() {
        super(DMR_READ_APPLICATION, ADDRESS_SWITCHYARD);
    }

    public ReadApplication(final String application) {
        this();
        addAdditionalProperty(PARAM_NAME, application);
    }
}
