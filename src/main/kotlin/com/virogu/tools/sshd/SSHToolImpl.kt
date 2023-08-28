package com.virogu.tools.sshd

import org.apache.sshd.client.ClientBuilder
import org.apache.sshd.client.SshClient
import org.apache.sshd.client.keyverifier.AcceptAllServerKeyVerifier
import org.apache.sshd.client.session.ClientSession
import org.apache.sshd.common.kex.BuiltinDHFactories
import org.apache.sshd.common.signature.BuiltinSignatures
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.stream.Collectors

class SSHToolImpl : SSHTool {

    private fun createClient() = SshClient.setUpDefaultClient().apply {
        signatureFactories = BuiltinSignatures.VALUES.toList()
        keyExchangeFactories = BuiltinDHFactories.VALUES.stream()
            .map(ClientBuilder.DH2KEX)
            .collect(Collectors.toList())
        SSHVerifyTools.keyPairs.forEach(::addPublicKeyIdentity)
        serverKeyVerifier = AcceptAllServerKeyVerifier.INSTANCE
    }

    override suspend fun connect(
        host: String,
        user: String,
        password: String,
        port: Int,
        timeout: Long,
        doOnConnected: suspend SSHTool.(ClientSession) -> Unit
    ): Result<Unit> = runCatching {
        createClient().use { client ->
            client.open()
            val session = client.connect(
                user, host, port
            ).verify(timeout).session
            session.apply {
                addPasswordIdentity(password)
            }.also {
                it.auth().verify(timeout)
            }
            session.use {
                doOnConnected(it)
            }
            client.close()
        }
    }

    override suspend fun exec(
        session: ClientSession,
        vararg cmds: String,
        charset: Charset
    ): Result<String> {
        val stdout = ConsoleOutputStream()
        val stderr = ConsoleOutputStream()
        cmds.forEach {
            session.exec(it, stdout, stderr, charset)
        }
        return if (stderr.size() > 0) {
            val errorMessage = stderr.toString(Charsets.UTF_8)
            Result.failure(IllegalStateException("error: $errorMessage"))
        } else {
            Result.success(stdout.toString(Charsets.UTF_8))
        }
    }

    private fun ClientSession.exec(
        cmd: String,
        stdout: ByteArrayOutputStream = ConsoleOutputStream(),
        stderr: ByteArrayOutputStream = ConsoleOutputStream(),
        charset: Charset = Charsets.UTF_8
    ): Result<String> = try {
        executeRemoteCommand(cmd, stdout, stderr, charset)
        if (stderr.size() > 0) {
            val errorMessage = stderr.toString(Charsets.UTF_8)
            Result.failure(IllegalStateException("Error reported from remote command=$cmd, error: $errorMessage"))
        } else {
            Result.success(stdout.toString(Charsets.UTF_8))
        }
    } catch (e: Throwable) {
        if (stderr.size() > 0) {
            val errorMessage = stderr.toString(Charsets.UTF_8)
            Result.failure(IllegalStateException("Error reported from remote command=$cmd, error: $errorMessage"))
        } else {
            Result.failure(e)
        }
    }

    override fun destroy() {

    }

    internal inner class ConsoleOutputStream : ByteArrayOutputStream() {
        override fun write(b: Int) {
            super.write(b)
            logger.debug(b.toChar().toString())
        }

        override fun write(b: ByteArray) {
            super.write(b)
            logger.debug(String(buf, Charsets.UTF_8))
        }

        override fun writeBytes(b: ByteArray?) {
            super.writeBytes(b)
            logger.debug(String(buf, Charsets.UTF_8))
        }

        override fun write(b: ByteArray, off: Int, len: Int) {
            super.write(b, off, len)
            logger.debug(String(buf, Charsets.UTF_8))
        }
    }

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(SSHToolImpl::class.java)
    }
}