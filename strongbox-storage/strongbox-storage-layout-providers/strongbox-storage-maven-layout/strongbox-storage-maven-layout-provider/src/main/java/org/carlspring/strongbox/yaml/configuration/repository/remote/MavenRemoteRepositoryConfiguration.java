package org.carlspring.strongbox.yaml.configuration.repository.remote;

import org.carlspring.strongbox.providers.layout.Maven2LayoutProvider;
import org.carlspring.strongbox.yaml.repository.remote.CustomRemoteRepositoryConfiguration;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import org.glassfish.hk2.api.Immediate;

@Immediate
@JsonTypeName(Maven2LayoutProvider.ALIAS)
@XmlAccessorType(XmlAccessType.FIELD)
public class MavenRemoteRepositoryConfiguration
        extends CustomRemoteRepositoryConfiguration
{

    MavenRemoteRepositoryConfiguration()
    {
    }

    MavenRemoteRepositoryConfiguration(MutableMavenRemoteRepositoryConfiguration delegate)
    {
    }

}
