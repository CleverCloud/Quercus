/*******************************************************************************
 * Copyright (c) 1998, 2010 Oracle. All rights reserved.
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 and Eclipse Distribution License v. 1.0
 * which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *     Caucho - initial implementation from Resin.
 ******************************************************************************/
package org.eclipse.persistence.platform.server.resin;

import org.eclipse.persistence.platform.server.ServerPlatformBase;
import org.eclipse.persistence.sessions.DatabaseSession;
import org.eclipse.persistence.transaction.resin.ResinTransactionController;

public class ResinPlatform extends ServerPlatformBase {

    public ResinPlatform(DatabaseSession newDatabaseSession) {
        super(newDatabaseSession);
    }

    @SuppressWarnings("unchecked")
    public Class getExternalTransactionControllerClass() {

            if (externalTransactionControllerClass == null){
                    externalTransactionControllerClass = ResinTransactionController.class;
            }
        return externalTransactionControllerClass;
    }
}
