package featuremodeltransformations_deprecated;

import java.io.File;
import java.util.List;

import com.lexicalscope.jewel.cli.CommandLineInterface;
import com.lexicalscope.jewel.cli.Option;
import com.lexicalscope.jewel.cli.Unparsed;

@CommandLineInterface(application="FeatureModelToCSP")
public interface FeatureModelToCSPOptions {
	
	@Option(description="Address of the clafer translator executable.", defaultValue="clafer")
	String getClaferTranslator();
	
	
	@Option(description="display this help and exit")
	boolean isHelp();
	
	@Unparsed(name="FILE", description="Feature model in clafer", exactly=1)
	String getClaferModel();
}
