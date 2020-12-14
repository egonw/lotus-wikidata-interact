package net.nprod.lotus.wdimport

import net.nprod.lotus.input.DataTotal
import net.nprod.lotus.input.Reference
import net.nprod.lotus.wdimport.wd.InstanceItems
import net.nprod.lotus.wdimport.wd.WDFinder
import net.nprod.lotus.wdimport.wd.interfaces.Publisher
import net.nprod.lotus.wdimport.wd.models.WDArticle
import org.apache.logging.log4j.LogManager


class ReferencesProcessor(
    val dataTotal: DataTotal,
    val publisher: Publisher,
    val wdFinder: WDFinder,
    val instanceItems: InstanceItems
) {
    private val logger = LogManager.getLogger(ReferencesProcessor::class.qualifiedName)
    private val articlesCache: MutableMap<Reference, WDArticle> = mutableMapOf()

    private fun articleFromReference(reference: Reference): WDArticle {
        val article = WDArticle(
            name = reference.title ?: reference.doi,
            title = reference.title,
            doi = reference.doi.toUpperCase(), // DOIs are always uppercase
        ).tryToFind(wdFinder, instanceItems)
        // TODO: Add PMID and PMCID
        publisher.publish(article, "upserting article")
        return article
    }

    fun get(key: Reference): WDArticle {
        return articlesCache.getOrPut(key) {
            articleFromReference(key)
        }
    }
}
