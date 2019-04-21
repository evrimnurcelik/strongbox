package org.carlspring.strongbox.providers.io;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.carlspring.strongbox.booters.PropertiesBooter;
import org.carlspring.strongbox.storage.repository.ImmutableRepository;
import org.carlspring.strongbox.storage.repository.MutableRepository;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Przemyslaw Fusik
 */
public class RepositoryPathTest
{

    private static final Path REPOSITORY_BASEDIR = Paths.get(new File("target/strongbox-vault/storages/storage0/releases").getAbsolutePath());

    private MutableRepository repository;

    private LayoutFileSystem repositoryFileSystem;


    @BeforeEach
    public void setup()
    {
        repository = new MutableRepository();
        repository.setBasedir(REPOSITORY_BASEDIR.toAbsolutePath().toString());

        PropertiesBooter propertiesBooter = new PropertiesBooter();
        repositoryFileSystem = new LayoutFileSystem(propertiesBooter, new ImmutableRepository(repository), FileSystems.getDefault(), null)
        {
            @Override
            public Set<String> getDigestAlgorithmSet()
            {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Test
    public void repositoryPathShouldNotResolveStringAbsolutePaths()
    {
        assertThrows(RepositoryRelativePathConstructionException.class, () -> {
            RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

            path.resolve("/absolute");
        });
    }

    @Test
    public void repositoryPathShouldNotResolvePathAbsolutePaths()
    {
        assertThrows(RepositoryRelativePathConstructionException.class,() -> {
            RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

            path.resolve(Paths.get("/absolute"));
        });
    }

    @Test
    public void repositoryPathShouldNotResolveSiblingStringAbsolutePaths()
    {
        assertThrows(RepositoryRelativePathConstructionException.class,() -> {
            RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

            path.resolveSibling("/absolute");
        });
    }

    @Test
    public void repositoryPathShouldNotResolveSiblingPathAbsolutePaths()
    {
        assertThrows(RepositoryRelativePathConstructionException.class,() -> {
            RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

            path.resolveSibling(Paths.get("/absolute"));
        });
    }

    @Test
    public void repositoryPathShouldResolveStringRelativePaths()
    {
        RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

        path = path.resolve("relative");

        assertThat(path, CoreMatchers.equalTo(REPOSITORY_BASEDIR.resolve("relative")));
    }

    @Test
    public void repositoryPathShouldResolvePathRelativePaths()
    {
        RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

        path = path.resolve(Paths.get("relative"));

        assertThat(path, CoreMatchers.equalTo(REPOSITORY_BASEDIR.resolve("relative")));
    }

    @Test
    public void shouldNotBePossibleToResolveSiblingStringOutsideTheRepositoryRoot()
    {
        assertThrows(PathExceededRootRepositoryPathException.class,() -> {
            RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

            path.resolveSibling("relative");
        });
    }

    @Test
    public void shouldNotBePossibleToResolveSiblingPathOutsideTheRepositoryRoot()
    {
        assertThrows(PathExceededRootRepositoryPathException.class,() -> {
            RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem);

            path.resolveSibling(Paths.get("relative"));
        });
    }


    @Test
    public void repositoryPathShouldResolveSiblingStringRelativePaths()
    {
        RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem).resolve("onePathFurther");

        path = path.resolveSibling("relative");

        assertThat(path, CoreMatchers.equalTo(REPOSITORY_BASEDIR.resolve("relative")));
    }

    @Test
    public void repositoryPathShouldResolveSiblingPathRelativePaths()
    {
        RepositoryPath path = new RepositoryPath(REPOSITORY_BASEDIR, repositoryFileSystem).resolve("onePathFurther");

        path = path.resolveSibling(Paths.get("relative"));

        assertThat(path, CoreMatchers.equalTo(REPOSITORY_BASEDIR.resolve("relative")));
    }


}
