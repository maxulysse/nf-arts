// A small pipeline with uneven tasks, a retry and a fan-in, to give nf-arts something to draw

process ALIGN {
    errorStrategy { task.attempt == 1 && id == 3 ? 'retry' : 'terminate' }
    maxRetries 1

    input:
    val id

    output:
    val id

    script:
    """
    sleep ${1 + (id % 4)}
    python3 -c "x = bytearray(${20 + id * 8} * 1024 * 1024)" 2>/dev/null || true
    [ ${id} -eq 3 ] && [ ${task.attempt} -eq 1 ] && exit 1 || true
    """
}

process CALL {
    input:
    val id

    output:
    val id

    script:
    """
    sleep ${2 + (id % 3)}
    """
}

process REPORT {
    input:
    val ids

    script:
    """
    sleep 2
    """
}

workflow {
    channel.of(1..12) | ALIGN | CALL | collect | REPORT
}
