/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * Copyright (c) 2020 Jonathan Bisson
 *
 */

package net.nprod.lotus.wikidata.upload.controllers

import kotlinx.serialization.Serializable
import net.nprod.lotus.wikidata.upload.jobs.LotusImportJob
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobExecution
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.explore.JobExplorer
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import kotlin.time.ExperimentalTime

/**
 * Maximum number of jobs to display
 */
const val MAX_NUMBER_JOBS = 10

@Serializable
data class JobData(
    val name: String,
    val createTime: String,
    val endTime: String,
    val isRunning: Boolean,
    val status: String
) {
    companion object {
        fun fromJobExecution(execution: JobExecution): JobData = JobData(
            execution.jobId.toString(),
            execution.createTime.toString(),
            execution.endTime?.toString() ?: "", // It can really be null, don't get fooled
            execution.isRunning,
            execution.status.toString()
        )
    }
}

@Controller
@ExperimentalTime
class JobController constructor(
    val lotusImportJob: LotusImportJob,
    @Qualifier("asyncJobLauncher") val jobLauncher: JobLauncher,
    @Qualifier("newJob") val job: Job,
    val jobExplorer: JobExplorer
) {
    private var lastJob: JobExecution? = null

    @GetMapping("/jobs", produces = ["application/json"])
    @ResponseBody
    fun getJobs(): String {
        val jobNames = jobExplorer.jobNames.toList()
        return jobNames.flatMap<String?, String> {
            jobExplorer.getJobInstances(it, 0, MAX_NUMBER_JOBS).flatMap {
                jobExplorer.getJobExecutions(it).map {
                    "id: ${it.jobId} create time: ${it.createTime} " +
                        "running: ${it.isRunning} exit Status ${it.exitStatus}"
                }
            }
        }.joinToString("|")
    }

    @GetMapping("/jobs/import/request", produces = ["application/json"])
    @ResponseBody
    @Suppress("TooGenericExceptionCaught", "PrintStackTrace")
    fun newRunjob(
        @RequestParam(name = "max_records") maxRecords: Long? = null,
        @RequestParam(name = "skip") skip: Long? = null
    ): JobData? {
        if (lastJob == null || lastJob?.status in arrayOf(
                BatchStatus.COMPLETED,
                BatchStatus.FAILED,
                BatchStatus.STOPPED
            )
        ) {
            val parameters = JobParametersBuilder().addLong("unique", System.nanoTime())
            maxRecords?.let { parameters.addLong("max_records", it) }
            skip?.let { if (it >= 0) parameters.addLong("skip", it) }
            try {
                lastJob = jobLauncher.run(job, parameters.toJobParameters())
            } catch (e: Exception) {
                lastJob?.status = BatchStatus.FAILED
                e.printStackTrace() // We catch everything, we may want to log that properly
            }
        }
        return lastJob?.let { JobData.fromJobExecution(it) }
    }
}
