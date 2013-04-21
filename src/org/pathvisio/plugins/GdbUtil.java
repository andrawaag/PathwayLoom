// PathVisio,
// a tool for data visualization and analysis using Biological Pathways
// Copyright 2006-2009 BiGCaT Bioinformatics
//
// Licensed under the Apache License, Version 2.0 (the "License"); 
// you may not use this file except in compliance with the License. 
// You may obtain a copy of the License at 
// 
// http://www.apache.org/licenses/LICENSE-2.0 
//  
// Unless required by applicable law or agreed to in writing, software 
// distributed under the License is distributed on an "AS IS" BASIS, 
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
// See the License for the specific language governing permissions and 
// limitations under the License.
//
package org.pathvisio.plugins;

import java.util.Set;

import org.bridgedb.IDMapper;
import org.bridgedb.IDMapperException;
import org.bridgedb.DataSource;
import org.bridgedb.Xref;
import org.pathvisio.core.data.GdbManager;
import org.pathvisio.core.util.Utils;

public class GdbUtil 
{

	/**
	 * Check if an Xref is from a certain DataSource.
	 * If it is, simply returns ref.
	 * If it isn't it tries to convert using gdbManager. If a proper gdb is loaded, the first
	 * possible match will be returned
	 * 
	 * @param ref input Xref to check and convert
	 * @param gdbManager reference to global GdbManager object, may be null (in which case no conversion can take place)
	 * @param DataSource
	 * @returns Xref that matches target DataSource or null if conversion failed
	 */
	static Xref forceDataSource (Xref ref, GdbManager gdbManager, DataSource dest) throws IDMapperException
	{
		if (ref.getDataSource() != dest)
		{
			if (gdbManager == null)
			{
				return null;
			}
			
			IDMapper gdb = gdbManager.getCurrentGdb();
			// use first cross-ref we find
			Set<Xref> refs = gdb.mapID(ref, dest);
			ref = Utils.oneOf(refs);
		}
		return ref;
	}

}
