/*
 * Copyright 2017, Pavel Kryl
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

package eu.kryl.android.common.hub;


/**
 * The parent of all routable interfaces in our @InterfaceHub.
 * Its purpose is to enforce setting the ID of child interfaces to:
 *     ID = IHBASE_CHILD.class.getName().hashCode();
 * Otherwise the InterfaceHub will pick up the ID defined here
 * and throw a runtime exception.
 */
public interface InterfaceHubHandler {
    long IH_ID_UNSET = -1;
    long ID = IH_ID_UNSET;
    
}
