package click.replicatedDataStore;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class PortRetryRule implements TestRule {
        private final int retryCount;

        public PortRetryRule(int retryCount) {
            this.retryCount = retryCount;
        }


        @Override
        public Statement apply(Statement statement, Description description) {
            return new Statement() {
                @Override
                public void evaluate() throws Throwable {
                    Throwable lastThrowable = null;
                    for (int i = 0; i < retryCount; i++) {
                        try {
                            statement.evaluate();
                            return;
                        } catch (Throwable t) {
                            lastThrowable = t;
                            TestUtils.getPort();
                            System.out.println("Retrying test " + description.getMethodName() + " (attempt " + (i + 1) + ")");
                        }
                    }
                    throw lastThrowable;
                }
            };
        }

}
