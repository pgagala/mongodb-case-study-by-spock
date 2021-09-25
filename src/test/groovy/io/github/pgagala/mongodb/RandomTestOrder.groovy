package io.github.pgagala.mongodb

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.spockframework.runtime.extension.AbstractAnnotationDrivenExtension
import org.spockframework.runtime.extension.ExtensionAnnotation
import org.spockframework.runtime.extension.IAnnotationDrivenExtension
import org.spockframework.runtime.model.SpecInfo

import java.lang.annotation.ElementType
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@ExtensionAnnotation(RandomExtension)
@interface RandomSingleSpecOrder {}

@Slf4j
@CompileStatic
final class RandomExtension implements IAnnotationDrivenExtension<RandomSingleSpecOrder> {

    @Override
    void visitSpecAnnotation(RandomSingleSpecOrder annotation, SpecInfo spec) {
        final List<Integer> order = (0..(spec.features.size())) as ArrayList
        Collections.shuffle(order, new Random(System.nanoTime()))

        spec.features.each { feature ->
            feature.executionOrder = order.pop()
        }
    }

}
