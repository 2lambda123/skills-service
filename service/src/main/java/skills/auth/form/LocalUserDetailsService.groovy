/**
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package skills.auth.form

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Conditional
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import skills.auth.SecurityMode
import skills.auth.UserInfoAndRoleService

@Conditional(SecurityMode.FormAuth)
@Service
class LocalUserDetailsService implements UserDetailsService {

    @Autowired
    UserInfoAndRoleService userInfoAccessor

    @Override
    UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        skills.auth.UserInfo userInfo = userInfoAccessor.loadByUserId(username)
        if (!userInfo) {
            throw new UsernameNotFoundException("Unknown user [$username]")
        }
        return userInfo
    }
}
