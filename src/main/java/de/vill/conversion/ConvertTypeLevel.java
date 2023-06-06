package de.vill.conversion;

import de.vill.model.Attribute;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.FeatureType;
import de.vill.model.Group;
import de.vill.model.LanguageLevel;
import de.vill.model.constraint.Constraint;
import de.vill.model.constraint.ExpressionConstraint;
import de.vill.model.expression.Expression;
import de.vill.model.expression.LiteralExpression;
import de.vill.util.Constants;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ConvertTypeLevel implements IConversionStrategy {

    @Override
    public Set<LanguageLevel> getLevelsToBeRemoved() {
        return new HashSet<>(Collections.singletonList(LanguageLevel.TYPE_LEVEL));
    }

    @Override
    public Set<LanguageLevel> getTargetLevelsOfConversion() {
        return new HashSet<>(Collections.singletonList(LanguageLevel.SMT_LEVEL));
    }

    @Override
    public void convertFeatureModel(final FeatureModel rootFeatureModel, final FeatureModel featureModel) {
        rootFeatureModel.getOwnConstraints().forEach(this::convertFeaturesInExpressionConstraint);
        this.traverseFeatures(featureModel.getRootFeature());
    }

    private void convertFeaturesInExpressionConstraint(final Constraint constraint) {
        if (constraint instanceof ExpressionConstraint) {
            for (final Expression expression : ((ExpressionConstraint) constraint).getExpressionSubParts()) {
                this.replaceFeatureInExpression(expression);
            }
        } else {
            for (final Constraint subConstraint : constraint.getConstraintSubParts()) {
                this.convertFeaturesInExpressionConstraint(subConstraint);
            }
        }
    }

    private void replaceFeatureInExpression(final Expression expression) {
        if (expression instanceof LiteralExpression) {
            if (((LiteralExpression) expression).getAttributeName() == null) {
                expression.replaceExpressionSubPart(expression, new LiteralExpression(
                    ((LiteralExpression) expression).getContent() + "." + Constants.TYPE_LEVEL_VALUE)
                );
            }
        }
        for (final Expression expr : expression.getExpressionSubParts()) {
            this.replaceFeatureInExpression(expr);
        }
    }

    private void traverseFeatures(final Feature feature) {
        if (feature.getFeatureType() != null) {
            feature.getAttributes().put(
                "feature_type",
                new Attribute<>("feature_type", feature.getFeatureType().getName())
            );
            feature.getAttributes().put(Constants.TYPE_LEVEL_VALUE, getDefaultAttribute(feature.getFeatureType()));
            feature.setFeatureType(null);
        }

        for (final Group group : feature.getChildren()) {
            for (final Feature subFeature : group.getFeatures()) {
                this.traverseFeatures(subFeature);
            }
        }
    }


    private Attribute getDefaultAttribute(FeatureType type) {
        if (type == FeatureType.BOOL) {
            return new Attribute<Boolean>(Constants.TYPE_LEVEL_VALUE, true);
        } else if (type == FeatureType.INT) {
            return new Attribute<Long>(Constants.TYPE_LEVEL_VALUE, 0l);
        } else if (type == FeatureType.REAL) {
            return new Attribute<Double>(Constants.TYPE_LEVEL_VALUE, 0d);
        } else  {
            return new Attribute<String>(Constants.TYPE_LEVEL_VALUE, "");
        }
    }

}