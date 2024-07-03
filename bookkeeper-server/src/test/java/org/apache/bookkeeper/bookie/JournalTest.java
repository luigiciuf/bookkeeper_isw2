package org.apache.bookkeeper.bookie;

import org.apache.bookkeeper.bookie.Journal;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.util.*;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doReturn;

@RunWith(Enclosed.class)
public class JournalTest {
    @RunWith(value = Parameterized.class)
    public static class GetJournalIdsTest{
        @Mock
        private File mockJournalDir;

        private File journalDir;
        private final Journal.JournalIdFilter journalIdFilter;
        private static Class<? extends Exception> expectedException;
        private final List<Long> expectedResult;

        // Enumerazione per i tipi di directory del journal
        public enum JournalDirType {
            ONE_LOG_DIR("src/test/resources/journals/logs_dir"),
            ONE_LOG_ONE_TEXT_DIR("src/test/resources/journals/logs_and_other_dir"),
            ONE_TEXT_DIR("src/test/resources/journals/other_files_dir"),
            NOT_EXISTING_DIR("src/test/resources/journals/not_existing_dir"),
            LOG_FILE("src/test/resources/journals/logs_dir/0.log"),
            VOID_DIR("src/test/resources/journals/void_dir"),
            ADF_DIR("src/test/resources/journals/adf_dir");

            private final String path;

            JournalDirType(String path) {
                this.path = path;
            }

            // Restituisce la directory del journal come file
            public File getJournalDir() {
                return new File(System.getProperty("user.dir"), path);
            }
        }
        public enum JournalIdFilterType {
            JOURNAL_ROLLING_FILTER(journalId -> journalId < 10),
            NEW_FILTER(journalId -> journalId > 0),
            ALWAYS_FALSE_FILTER(journalId -> false),
            ADF_FILTER(journalId ->journalId > 1);





            private final Journal.JournalIdFilter filter;

            JournalIdFilterType(Journal.JournalIdFilter filter) {
                this.filter = filter;
            }

            public Journal.JournalIdFilter getJournalIdFilter() {
                return filter;
            }
        }

        public GetJournalIdsTest(List<Long> expectedResult, Class<? extends Exception> expectedException, File journalDir, Journal.JournalIdFilter journalIdFilter) {
            this.expectedResult = expectedResult;
            this.expectedException = expectedException;
            this.journalDir = journalDir;
            this.journalIdFilter = journalIdFilter;
        }
        @Parameterized.Parameters
        public static Collection<Object[]> getTestParameters() {
            return Arrays.asList(new Object[][]{
                    {Collections.singletonList(1L), null, JournalDirType.ONE_LOG_DIR.getJournalDir(), JournalIdFilterType.JOURNAL_ROLLING_FILTER.getJournalIdFilter()},
                    {Collections.singletonList(1L), null, JournalDirType.ONE_LOG_DIR.getJournalDir(), null},
                    {Collections.emptyList(), null, JournalDirType.NOT_EXISTING_DIR.getJournalDir(), JournalIdFilterType.NEW_FILTER.getJournalIdFilter()},
                    {Collections.emptyList(), null, JournalDirType.LOG_FILE.getJournalDir(), null},
                    {null, Exception.class, null, null},
                    {Collections.emptyList(), null, JournalDirType.VOID_DIR.getJournalDir(), null},
                    {Collections.singletonList(1L), null, JournalDirType.ONE_LOG_ONE_TEXT_DIR.getJournalDir(), null},
                    {Collections.emptyList(), null, JournalDirType.ONE_LOG_ONE_TEXT_DIR.getJournalDir(), JournalIdFilterType.ALWAYS_FALSE_FILTER.getJournalIdFilter()},
                    {new ArrayList<>(Arrays.asList(1L, 2L)), null, JournalDirType.ADF_DIR.getJournalDir(), null}
            });
        }
        @Before
        public void setUp() {
            MockitoAnnotations.initMocks(this);

            if (journalDir != null && journalDir.getPath().substring(journalDir.getPath().length() - 8).equals("void_dir")) {
                boolean success = journalDir.mkdir();
                if (!success) {
                    System.out.println("Could not create void dir");
                } else {
                    System.out.println("Created void dir");
                }
            }

            if (journalDir != null && journalDir.getPath().equals(JournalDirType.ADF_DIR.getJournalDir().getPath())) {
                // Creo un array di file mockati con nomi non ordinati
                File mockFile1 = Mockito.mock(File.class);
                File mockFile2 = Mockito.mock(File.class);
                Mockito.when(mockFile1.getName()).thenReturn("2.txn");
                Mockito.when(mockFile2.getName()).thenReturn("1.txn");

                // Metto i file mockati in un array
                File[] mockFiles = new File[]{mockFile1, mockFile2};

                // Creo uno spy su journalDir e mocko il metodo listFiles()
                journalDir = Mockito.spy(journalDir);
                doReturn(mockFiles).when(journalDir).listFiles();
            }
        }

        @Test
        public void getJournalIdsTest() {
            try {
                System.out.println("Journal dir:\n" + journalDir.getPath());
                System.out.println("Journal dir list files:\n" + Arrays.toString(journalDir.listFiles()));

                // Chiama il metodo da testare
                List<Long> journalIds = Journal.listJournalIds(journalDir, journalIdFilter);

                System.out.println("Result:\n" + journalIds);
                System.out.println("Expected result:\n" + expectedResult);

                // Verifica che il risultato ottenuto corrisponda a quello atteso
                assertTrue(journalIds.equals(expectedResult));
            } catch (Exception e) {
                System.out.println("Exception:\n" + e);
                // Verifica che l'eccezione ottenuta sia del tipo atteso
                if (expectedException != null && expectedException.isAssignableFrom(e.getClass())) {
                    assertTrue(true);
                } else {
                    fail();
                }
            }
        }


    }

}
