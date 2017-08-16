package org.streampipes.pe.processors.esper.filter.text;

import org.streampipes.container.util.StandardTransportFormat;
import org.streampipes.model.impl.EpaType;
import org.streampipes.model.impl.EventSchema;
import org.streampipes.model.impl.EventStream;
import org.streampipes.model.impl.eventproperty.EventProperty;
import org.streampipes.model.impl.graph.SepaDescription;
import org.streampipes.model.impl.graph.SepaInvocation;
import org.streampipes.model.impl.output.OutputStrategy;
import org.streampipes.model.impl.output.RenameOutputStrategy;
import org.streampipes.model.impl.staticproperty.FreeTextStaticProperty;
import org.streampipes.model.impl.staticproperty.MappingPropertyUnary;
import org.streampipes.model.impl.staticproperty.OneOfStaticProperty;
import org.streampipes.model.impl.staticproperty.Option;
import org.streampipes.model.impl.staticproperty.StaticProperty;
import org.streampipes.model.util.SepaUtils;
import org.streampipes.pe.processors.esper.config.EsperConfig;
import org.streampipes.pe.processors.esper.util.StringOperator;
import org.streampipes.sdk.StaticProperties;
import org.streampipes.sdk.helpers.EpRequirements;
import org.streampipes.wrapper.ConfiguredEventProcessor;
import org.streampipes.wrapper.runtime.EventProcessor;
import org.streampipes.wrapper.standalone.declarer.StandaloneEventProcessorDeclarerSingleton;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class TextFilterController extends StandaloneEventProcessorDeclarerSingleton<TextFilterParameter> {
	
	@Override
	public SepaDescription declareModel() {
			
		List<EventProperty> eventProperties = new ArrayList<EventProperty>();	
		EventProperty property = EpRequirements.stringReq();
		
		eventProperties.add(property);
		
		EventSchema schema1 = new EventSchema();
		schema1.setEventProperties(eventProperties);
		
		EventStream stream1 = new EventStream();
		stream1.setEventSchema(schema1);
		
		SepaDescription desc = new SepaDescription("textfilter", "Text Filter", "Text Filter Description");
		desc.setSupportedGrounding(StandardTransportFormat.getSupportedGrounding());
		desc.setCategory(Arrays.asList(EpaType.FILTER.name()));
		desc.setIconUrl(EsperConfig.iconBaseUrl + "/Textual_Filter_Icon_HQ.png");
		
		//TODO check if needed
		stream1.setUri(EsperConfig.serverUrl +"/" +desc.getElementId());
		desc.addEventStream(stream1);
		List<OutputStrategy> strategies = new ArrayList<OutputStrategy>();
		strategies.add(new RenameOutputStrategy("Enrich", "EnrichedMovementAnalysis"));
		desc.setOutputStrategies(strategies);
		
		List<StaticProperty> staticProperties = new ArrayList<StaticProperty>();
		
		OneOfStaticProperty operation = new OneOfStaticProperty("operation", "Select Operation", "");
		operation.addOption(new Option("MATCHES"));
		operation.addOption(new Option("CONTAINS"));
		staticProperties.add(operation);
		try {
			staticProperties.add(new MappingPropertyUnary(new URI(property.getElementName()), "text", "Select Text Property", ""));
		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		staticProperties.add(StaticProperties.stringFreeTextProperty("keyword", "Select Keyword", ""));
		desc.setStaticProperties(staticProperties);
		
		return desc;
	}

	@Override
	public ConfiguredEventProcessor<TextFilterParameter, EventProcessor<TextFilterParameter>> onInvocation
					(SepaInvocation sepa) {
		String keyword = ((FreeTextStaticProperty) (SepaUtils
						.getStaticPropertyByInternalName(sepa, "keyword"))).getValue();
		String operation = SepaUtils.getOneOfProperty(sepa,
						"operation");
		String filterProperty = SepaUtils.getMappingPropertyName(sepa,
						"text");

		logger.info("Text Property: " +filterProperty);

		TextFilterParameter staticParam = new TextFilterParameter(sepa,
						keyword,
						StringOperator.valueOf(operation),
						filterProperty);

		return new ConfiguredEventProcessor<>(staticParam, TextFilter::new);
	}
}
