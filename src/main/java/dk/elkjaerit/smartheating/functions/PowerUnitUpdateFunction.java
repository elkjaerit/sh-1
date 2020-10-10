package dk.elkjaerit.smartheating.functions;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import dk.elkjaerit.smartheating.powerunit.PowerUnitClient;
import dk.elkjaerit.smartheating.powerunit.PowerUnitUpdater;
import org.apache.commons.lang3.RandomUtils;

public class PowerUnitUpdateFunction implements BackgroundFunction<Consumer.PubSubMessage> {
    @Override
    public void accept(Consumer.PubSubMessage pubSubMessage, Context context) throws Exception {
        PowerUnitUpdater.update();
    }
}
