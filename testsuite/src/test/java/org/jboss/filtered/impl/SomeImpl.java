/*
 * Copyright 2011 Red Hat inc. and third party contributors as noted 
 * by the author tags.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.filtered.impl;

import java.lang.reflect.Method;

import org.jboss.filtered.api.SomeAPI;
import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoader;

/**
 * @author <a href="mailto:ales.justin@jboss.org">Ales Justin</a>
 */
public class SomeImpl extends SomeAPI {
    protected String onward() {
        return "Onward ";
    }

    public String go(String arg) throws Exception {
        ModuleClassLoader mcl = (ModuleClassLoader) getClass().getClassLoader();
        ModuleLoader ml = mcl.getModule().getModuleLoader();
        ClassLoader cl = ml.loadModule(ModuleIdentifier.create("ceylon.io", "0.5")).getClassLoader();

        Class<?> pp = cl.loadClass("ceylon.file.parsePath_");
        Method parse = pp.getDeclaredMethod("parsePath", String.class);
        Object path = parse.invoke(null, "buuu");
        Class<?> pc = cl.loadClass("ceylon.file.Path");
        Method r = pc.getDeclaredMethod("getResource");
        Class<?> rc = cl.loadClass("ceylon.file.Resource");
        Class<?> nof = cl.loadClass("ceylon.io.newOpenFile_");
        Method newOpenFile = nof.getDeclaredMethod("newOpenFile", rc);
        Object ofi = newOpenFile.invoke(null, r.invoke(path));
        System.out.println("ofi = " + ofi);

        return onward() + arg + "!";
    }
}
