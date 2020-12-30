/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Copyright (c) 2020 Jonathan Bisson
 *
 */

package net.nprod.lotus.wdimport.wd.models

import net.nprod.lotus.wdimport.wd.InstanceItems
import net.nprod.lotus.wdimport.wd.publishing.RemoteItem
import net.nprod.lotus.wdimport.wd.publishing.RemoteProperty
import org.joda.time.DateTime
import org.wikidata.wdtk.datamodel.helpers.Datamodel
import org.wikidata.wdtk.datamodel.interfaces.TimeValue
import org.wikidata.wdtk.datamodel.interfaces.Value

interface ReferenceableStatement {
    var property: RemoteProperty
    val preReferences: MutableList<WDPreReference>
    val overwritable: Boolean
}

data class ReferencableValueStatement(
    override var property: RemoteProperty,
    var value: Value,
    override val preReferences: MutableList<WDPreReference> = mutableListOf(),
    override val overwritable: Boolean = false
) : ReferenceableStatement {
    fun reference(property: RemoteProperty, value: Value): Boolean =
        preReferences.add(WDPreReference().add(property, value))

    fun statedIn(value: Value): Boolean = preReferences.add(WDPreReference().add(InstanceItems::statedIn, value))

    constructor(property: RemoteProperty, value: String, overwritable: Boolean = false) : this(
        property,
        Datamodel.makeStringValue(value),
        overwritable = overwritable
    )

    companion object {
        fun monolingualValue(property: RemoteProperty, value: String): ReferencableValueStatement =
            ReferencableValueStatement(property, Datamodel.makeMonolingualTextValue(value, "en"))

        fun datetimeValue(property: RemoteProperty, value: DateTime): ReferencableValueStatement =
            ReferencableValueStatement(
                property,
                Datamodel.makeTimeValue(
                    value.year.toLong(),
                    value.monthOfYear.toByte(),
                    value.dayOfMonth.toByte(),
                    TimeValue.CM_GREGORIAN_PRO
                )
            )
    }
}

data class ReferenceableRemoteItemStatement(
    override var property: RemoteProperty,
    var value: RemoteItem,
    override val preReferences: MutableList<WDPreReference> = mutableListOf(),
    override val overwritable: Boolean = false
) : ReferenceableStatement {

    fun reference(property: RemoteProperty, value: Value): Boolean =
        preReferences.add(WDPreReference().add(property, value))

    fun statedIn(value: Value): Boolean = preReferences.add(WDPreReference().add(InstanceItems::statedIn, value))
}
