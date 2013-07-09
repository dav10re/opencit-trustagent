/*
 * Copyright (C) 2012 Intel Corporation
 * All rights reserved.
 */
package com.intel.mtwilson.setup.cmd;

import com.intel.mountwilson.as.common.ASConfig;
import com.intel.mtwilson.as.controller.MwKeystoreJpaController;
import com.intel.mtwilson.as.controller.exceptions.NonexistentEntityException;
import com.intel.mtwilson.as.data.MwKeystore;
import com.intel.mtwilson.crypto.CryptographyException;
import com.intel.mtwilson.crypto.RsaUtil;
import com.intel.mtwilson.crypto.SimpleKeystore;
import com.intel.mtwilson.io.ByteArrayResource;
import com.intel.mtwilson.io.CopyResource;
import com.intel.mtwilson.io.FileResource;
import com.intel.mtwilson.setup.Command;
import com.intel.mtwilson.setup.SetupContext;
import com.intel.mtwilson.setup.SetupException;
import com.intel.mtwilson.util.ResourceFinder;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyManagementException;
import java.security.KeyPair;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.RandomStringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jbuhacoff
 */
public class GenerateSamlSigningKey implements Command {
    private Logger log = LoggerFactory.getLogger(getClass());
    
    public static final String SAML_KEYSTORE_FILE_CONF_KEY = "saml.keystore.file";
    public static final String SAML_KEYSTORE_PASSWORD_CONF_KEY = "saml.keystore.password";
    public static final String SAML_KEY_ALIAS_CONF_KEY = "saml.key.alias";
    public static final String SAML_KEY_PASSWORD_CONF_KEY = "saml.key.password";
    
    public static final String DEFAULT_SAML_CERTIFICATE_NAME = "CN=Attestation Service, OU=Mt Wilson, C=US";
    
    private Configuration conf = null;
    private String samlKeystoreFileRC2 = null;
    private String samlKeystorePassword = null;
    private String samlKeyAlias = null;
    private String samlKeyPassword = null;
    
//    private MwKeystoreJpaController keystoreJpa = null;
//    private MwKeystore mwKeystore = null;
    private ByteArrayResource keystoreResource;
    private SimpleKeystore keystore = null;

    private SetupContext ctx = null;

    @Override
    public void setContext(SetupContext ctx) {
        this.ctx = ctx;
    }
    
    private Configuration options = null;
    @Override
    public void setOptions(Configuration options) {
        this.options = options;
    }

    @Override
    public void execute(String[] args) throws Exception {
        setupConfiguration();
        setupDataAccess();
        if( samlKeystoreFileRC2 != null ) {
            migrateKeystoreFileToDatabase();
        }
        try {
            openKeystore();
        }
        catch(KeyManagementException e) {
            throw new SetupException("Cannot open SAML keystore", e);
        }
        if( keystore == null ) {
            try {
                createKeystore();
            }
            catch(Exception e) {
                throw new SetupException("Cannot create SAML keystore", e);
            }
        }
    }
    
    private void setupConfiguration() {
        conf = ASConfig.getConfiguration(); // XXX TODO:  configuration needs to be passed to us from the caller... could be a UI, or command line, etc. it is NOT global; this command could be executed multiple times for different servers in a mt wilson cluster, each having its own SAML key but they are all signed by the same CA ...  also the CA private key & password could be local (desktop app) and not be on the server at all !!
        samlKeystoreFileRC2 = conf.getString(SAML_KEYSTORE_FILE_CONF_KEY);
        samlKeystorePassword = conf.getString(SAML_KEYSTORE_PASSWORD_CONF_KEY);
        if( samlKeystorePassword == null ) {
            samlKeystorePassword = RandomStringUtils.randomAscii(16);
            conf.setProperty(SAML_KEYSTORE_PASSWORD_CONF_KEY, samlKeystorePassword);
            log.info("Generated SAML Keystore Password");
        }
        samlKeyAlias = conf.getString(SAML_KEY_ALIAS_CONF_KEY);
        if( samlKeyAlias == null ) {
            samlKeyAlias = "samlkey1";
            conf.setProperty(SAML_KEY_ALIAS_CONF_KEY, samlKeyAlias);
            log.info("Using default SAML Key Alias: {}", samlKeyAlias);
        }
        samlKeyPassword = conf.getString(SAML_KEY_PASSWORD_CONF_KEY); 
        if( samlKeyPassword == null ) {
            samlKeyPassword = RandomStringUtils.randomAscii(16);
            conf.setProperty(SAML_KEY_PASSWORD_CONF_KEY, samlKeyPassword);
            log.info("Generated SAML Key Password");
        }
        // XXX do we need to save conf??? how???
    }
    
    private void setupDataAccess() {
//        keystoreJpa = new MwKeystoreJpaController(dao.getEntityManagerFactory());  // only if you want keystores in the database. right now saml keystore will still be on disk, and we use a CA to keep things organized instead of having each server use the same private key.
    }
    
    /**
     * Precondition:  setupConfiguration() and setupDataAccess() and samlKeystoreFileRC2 != null
     */
    private void migrateKeystoreFileToDatabase() {
        try {
            File keystoreFile = ResourceFinder.getFile(samlKeystoreFileRC2);
            FileResource in = new FileResource(keystoreFile);
            ByteArrayResource out = new ByteArrayResource();
            CopyResource copy = new CopyResource(in, out);
            keystore = new SimpleKeystore(copy, samlKeystorePassword);
            keystore.save(); // will copy it to the byte array resource
//            mwKeystore = new MwKeystore();
//            mwKeystore.setKeystore(out.toByteArray());
//            mwKeystore.setName(HostTrustBO.SAML_KEYSTORE_NAME);
//            keystoreJpa.create(mwKeystore);
            conf.clearProperty(SAML_KEYSTORE_FILE_CONF_KEY);
//            log.info("Migrated RC2 SAML Keystore File to Database Keystore: {}", HostTrustBO.SAML_KEYSTORE_NAME);
        }
        catch(KeyManagementException e) {
            log.error("Cannot open RC2 SAML Keystore File", e);
        }
        catch(FileNotFoundException e) {
            log.error("Cannot open RC2 SAML Keystore File", e);
        }
        catch(KeyStoreException e) {
            log.error("Cannot save Keystore", e);
        }
        catch(IOException e) {
            log.error("Cannot save Keystore", e);
        }
        catch(NoSuchAlgorithmException e) {
            log.error("Cannot save Keystore", e);
        }
        catch(CertificateException e) {
            log.error("Cannot save Keystore", e);
        }
    }
    
    /**
     * Precondition:  setupConfiguration() and setupDataAccess()
     * 
     * @throws KeyManagementException
     */
    private void openKeystore() throws KeyManagementException {
//        mwKeystore = keystoreJpa.findMwKeystoreByName(HostTrustBO.SAML_KEYSTORE_NAME);
//        if( mwKeystore != null && mwKeystore.getKeystore() != null ) {
//            keystoreResource = new ByteArrayResource(mwKeystore.getKeystore());
            keystoreResource = new ByteArrayResource(); // XXX TODO: should we be loading it from somewhere ????
            keystore = new SimpleKeystore(keystoreResource, samlKeystorePassword);
//            log.info("Loaded SAML Keystore from database");
//        }
    }
    
    /**
     * Precondition:  setupConfiguration() and setupDataAccess()
     * 
     * @throws NoSuchAlgorithmException
     * @throws GeneralSecurityException
     * @throws IOException
     * @throws NonexistentEntityException
     * @throws Exception 
     */
    private void createKeystore() throws NoSuchAlgorithmException, GeneralSecurityException, IOException, NonexistentEntityException, CryptographyException {
        keystoreResource = new ByteArrayResource();
        // generate an RSA keypair and certificate
        KeyPair keypair = RsaUtil.generateRsaKeyPair(RsaUtil.MINIMUM_RSA_KEY_SIZE); // NoSuchAlgorithmException
        X509Certificate samlCert = RsaUtil.generateX509Certificate(DEFAULT_SAML_CERTIFICATE_NAME, keypair, RsaUtil.DEFAULT_RSA_KEY_EXPIRES_DAYS); // GeneralSecurityException, IOException
        keystore = new SimpleKeystore(keystoreResource, samlKeystorePassword);
        keystore.addKeyPairX509(keypair.getPrivate(), samlCert, samlKeyAlias, samlKeyPassword);
        keystore.save(); // into the resource.   IOException
        // save it to database
//        mwKeystore.setKeystore(keystoreResource.toByteArray());
//        keystoreJpa.edit(mwKeystore); // NonexistentEntityException, Exception
        
    }
    
    
}
