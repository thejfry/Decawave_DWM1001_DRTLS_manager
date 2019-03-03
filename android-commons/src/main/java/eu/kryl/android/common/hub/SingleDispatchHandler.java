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
 * If handler inherits from this one, the invocation is
 * dispatched only once. The latest registered instance will
 * dispatch the request. Other registered instances are silently
 * ignored.
 * 
 * @author pavel
 *
 */
public interface SingleDispatchHandler extends InterfaceHubHandler {

}
