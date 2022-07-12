package de.vill.main;

import de.vill.UVLLexer;
import de.vill.UVLParser;
import de.vill.exception.ParseError;
import de.vill.model.Feature;
import de.vill.model.FeatureModel;
import de.vill.model.Import;
import de.vill.model.LiteralConstraint;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.ParseTreeWalker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class UVLModelFactory {

    public FeatureModel parse(String text, Map<String, String> fileLoader) throws ParseError{
        FeatureModel featureModel = parseFeatureModelWithImports(text,fileLoader, new HashMap<>());
        composeFeatureModelFromImports(featureModel);
        referenceFeaturesInConstraints(featureModel);
        return featureModel;
    }

    public FeatureModel parseFeatureModelWithImports(String text, Map<String, String> fileLoader, Map<String, Import> visitedImports){
        UVLLexer uvlLexer = new UVLLexer(CharStreams.fromString(text));
        CommonTokenStream tokens = new CommonTokenStream(uvlLexer);
        UVLParser uvlParser = new UVLParser(tokens);

        uvlParser.addErrorListener(new BaseErrorListener(){
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
                throw new ParseError(line, charPositionInLine,"failed to parse at line " + line + ":" + charPositionInLine + " due to " + msg, e);
            }
        });

        UVLListener uvlListener = new UVLListener();
        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(uvlListener, uvlParser.featureModel());

        FeatureModel featureModel = uvlListener.getFeatureModel();
        featureModel.getOwnConstraints().addAll(featureModel.getConstraints());


        visitedImports.put(featureModel.getNamespace(), null);

        for(Import importLine : featureModel.getImports()){
            if(visitedImports.containsKey(importLine.getNamespace()) && visitedImports.get(importLine.getNamespace()) == null){
                throw new ParseError(0,0, "Cyclic import detected! " + "The import of " + importLine.getNamespace() + " in " + featureModel.getNamespace() + " creates a cycle", null);
            }else {
                try {
                    String path = fileLoader.get(importLine.getNamespace());
                    Path filePath = Path.of(path);
                    String content = Files.readString(filePath);
                    FeatureModel subModel = parseFeatureModelWithImports(content, fileLoader, visitedImports);
                    importLine.setFeatureModel(subModel);
                    visitedImports.put(importLine.getNamespace(), importLine);
                    featureModel.getConstraints().addAll(subModel.getConstraints());

                    for (Map.Entry<String, Feature> entry : subModel.getFeatureMap().entrySet()) {
                        if(entry.getValue().getNameSpace() == null){
                            entry.getValue().setNameSpace(importLine.getAlias());
                        }else {
                            entry.getValue().setNameSpace(importLine.getAlias() + "." + entry.getValue().getNameSpace());
                        }
                        if(!featureModel.getFeatureMap().containsKey(entry.getValue().getNameSpace() + "." + entry.getValue().getNAME())) {
                            featureModel.getFeatureMap().put(entry.getValue().getNameSpace() + "." + entry.getValue().getNAME(), entry.getValue());
                        }
                    }

                    //featureModel.getFeatureMap().putAll(subModel.getFeatureMap());
                } catch (IOException e) {
                    throw new ParseError(0, 0, "Could not resolve import: " + e.getMessage(), e);
                }
            }
        }

        return featureModel;
    }

    private void composeFeatureModelFromImports(FeatureModel featureModel){
        for (Map.Entry<String, Feature> entry : featureModel.getFeatureMap().entrySet()) {
            if(entry.getValue().isImported()){
                Feature oldFeature = entry.getValue();
                int lastDotIndex = oldFeature.getNAME().lastIndexOf(".");
                String subModelName = oldFeature.getNAME().substring(0, lastDotIndex);
                String featureName = oldFeature.getNAME().substring(lastDotIndex + 1, oldFeature.getNAME().length());

                Import relatedImport = oldFeature.getRelatedImport();

                Feature newFeature = relatedImport.getFeatureModel().getRootFeature();
                newFeature.setNameSpace(oldFeature.getNameSpace());
                oldFeature.getChildren().addAll(newFeature.getChildren());
                oldFeature.getAttributes().putAll(newFeature.getAttributes());
                relatedImport.getFeatureModel().setRootFeature(oldFeature);
            }
        }
    }

    private List<FeatureModel> createSubModelList(FeatureModel featureModel){
        var subModelList = new LinkedList<FeatureModel>();
        for(Import importLine : featureModel.getImports()){
            subModelList.add(importLine.getFeatureModel());
            subModelList.addAll(createSubModelList(importLine.getFeatureModel()));
        }
        return subModelList;
    }

    private void referenceFeaturesInConstraints(FeatureModel featureModel){
        var subModelList = createSubModelList(featureModel);
        var literalConstraints = featureModel.getLiteralConstraints();
        for(LiteralConstraint constraint : literalConstraints){
            Feature referencedFeature = featureModel.getFeatureMap().get(constraint.getLiteral());
            if(referencedFeature == null){
                throw new ParseError(0,0,"Feature " + constraint + " is referenced in a constraint in" + featureModel.getNamespace() + " but does not exist as feature in the tree!",null);
            }else {
                constraint.setFeature(referencedFeature);
            }
        }
        for(FeatureModel subModel : subModelList){
            literalConstraints = subModel.getLiteralConstraints();
            for(LiteralConstraint constraint : literalConstraints){
                Feature referencedFeature = subModel.getFeatureMap().get(constraint.getLiteral());
                if(referencedFeature == null){
                    throw new ParseError(0,0,"Feature " + constraint + " is referenced in a constraint in" + subModel.getNamespace() + " but does not exist as feature in the tree!",null);
                }else {
                    constraint.setFeature(referencedFeature);
                }
            }
        }
    }
}
