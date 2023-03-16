/*
 * Copyright (c) 2023.
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

package org.modelix.security

import org.springframework.core.convert.converter.Converter
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.stream.Collectors


class KeycloakRoleConverter:Converter<Jwt,Collection<GrantedAuthority>> {
    override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
        val realmAccess = jwt.claims["realm_access"] as Map<String,Any>
        if (realmAccess.isEmpty()){
            return ArrayList()
        }

        val roles = realmAccess["roles"] as List<String>

        return roles.stream().map { roleName:String -> "ROLE_$roleName" }
            .map { role:String -> SimpleGrantedAuthority(role) }
            .collect(Collectors.toList())

    }

}