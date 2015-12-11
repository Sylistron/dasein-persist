/**
 * Copyright (C) 1998-2011 enStratusNetworks LLC
 *
 * ====================================================================
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
 * ====================================================================
 */

package org.dasein.persist;

import org.apache.log4j.Logger;
import org.dasein.util.CachedItem;

public final class RelationalReleaseCache<T extends CachedItem> extends RelationalCache<T> {
    static public final Logger logger = Logger.getLogger(RelationalReleaseCache.class);
    
    public RelationalReleaseCache() { 
    	Thread t = new Thread() {

			@Override
			public void run() {
				if (logger.isDebugEnabled()) {
					logger.debug("enter");
				}
				
				try {
					while (true) {
						sleep(60 * 60 * 1000); // wait an hour TODO make this configurable
						if (logger.isDebugEnabled()) {
							logger.debug("releasing...");
						}
						getCache().releaseAll();
						if (logger.isDebugEnabled()) {
							logger.debug("released.");
						}
					}

				} catch (InterruptedException e) {
					// no worries, just die!
				}
				
				if (logger.isDebugEnabled()) {
					logger.debug("exit");
				}
			}
    	};
    	t.setDaemon(false);
        t.setName("DASEIN RELATIONAL CACHE FLUSHER");
    	t.start();
    }
}
