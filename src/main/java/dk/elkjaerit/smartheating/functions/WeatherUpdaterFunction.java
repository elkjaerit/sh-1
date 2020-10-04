package dk.elkjaerit.smartheating.functions;

import com.google.cloud.functions.BackgroundFunction;
import com.google.cloud.functions.Context;
import dk.elkjaerit.smartheating.WeatherUpdater;

public class WeatherUpdaterFunction implements BackgroundFunction<Consumer.PubSubMessage> {

  @Override
  public void accept(Consumer.PubSubMessage pubSubMessage, Context context) throws Exception {
    WeatherUpdater.run();
  }

}
