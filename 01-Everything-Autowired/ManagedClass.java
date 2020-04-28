import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@IndexAnnotated
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface ManagedClass {
}

