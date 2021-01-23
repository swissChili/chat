/*
Decentralized chat software
Copyright (C) 2021  swissChili

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package sh.swisschili.chat.client;

import sh.swisschili.chat.util.ChatProtos.UserStatus;

import java.util.Comparator;

public class UserStatusComparator implements Comparator<UserStatus> {
    @Override
    public int compare(UserStatus o1, UserStatus o2) {
        return o1.getUser().getName().compareTo(o2.getUser().getName());
    }
}
