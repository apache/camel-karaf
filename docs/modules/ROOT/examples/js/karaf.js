/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const KARAF_GROUPID = 'org.apache.camel.karaf'

module.exports = {
  v: function (name, kind, $) {
    return $[kind][name]
  },

  extensionRef: function (version, module, kind, $) {
    const thing = $[kind]
    const name = thing.name.startsWith('bindy') ? 'bindy' : thing.name
    const pageName = kind === 'other' ? name : `${name}-${kind}`
    if (thing.groupId === KARAF_GROUPID) {
      return `xref:${pageName}.adoc[${thing.title}]`
    }
    return `xref:${version}@components:${module}:${pageName}.adoc[${thing.title}]`
  },

  deprecatedFilterComponent: function (items) {
    return deprecatedFilter(items, 'component')
  },

  sortComponent: function (items) {
    return sort(items, 'component')
  },

  deprecatedFilterDataformat: function (items) {
    return deprecatedFilter(items, 'dataformat')
  },

  sortDataformat: function (items) {
    return sort(items, 'dataformat')
  },

  deprecatedFilterLanguage: function (items) {
    return deprecatedFilter(items, 'language')
  },

  sortLanguage: function (items) {
    return sort(items, 'language')
  },

  deprecatedFilterMiscellaneous: function (items) {
    return deprecatedFilter(items, 'other')
  },

  sortMiscellaneous: function (items) {
    return sort(items, 'other')
  },
}

function deprecatedFilter (items, kind) {
  return items.filter((item) => item.$[kind].deprecated === true)
}

function sort (items, kind) {
  return items.sort((f1, f2) => {
      const t1 = f1.$[kind].title.toLowerCase()
      const t2 = f2.$[kind].title.toLowerCase()
      return t1 < t2 ? -1 : t1 > t2 ? 1 : 0
    }
  )
}
