package com.intalio.web.profile.impl;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.codehaus.jackson.JsonParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.intalio.epn.impl.EpnJsonMarshaller;
import com.intalio.epn.impl.EpnJsonUnmarshaller;
import com.intalio.web.plugin.IDiagramPlugin;
import com.intalio.web.plugin.impl.PluginServiceImpl;
import com.intalio.web.profile.IDiagramProfile;

/**
 * The implementation of the epn profile for Process Designer.
 * @author Tihomir Surdilovic
 */
public class EpnProfileImpl implements IDiagramProfile {

    private static Logger _logger = LoggerFactory.getLogger(EpnProfileImpl.class);
    private Map<String, IDiagramPlugin> _plugins = new LinkedHashMap<String, IDiagramPlugin>();
    
    private String _stencilSet;
    private String _externalLoadURL;
    private String _usr;
    private String _pwd;
    
    public EpnProfileImpl(ServletContext servletContext) {
        this(servletContext, true);
    }
    
    public EpnProfileImpl(ServletContext servletContext, boolean initializeLocalPlugins) {
        if (initializeLocalPlugins) {
            initializeLocalPlugins(servletContext);
        }
    }
    
    private void initializeLocalPlugins(ServletContext context) {
        Map<String, IDiagramPlugin> registry = PluginServiceImpl.getLocalPluginsRegistry(context);
        //we read the default.xml file and make sense of it.
        FileInputStream fileStream = null;
        try {
            try {
                fileStream = new FileInputStream(new StringBuilder(context.getRealPath("/")).append("/").
                        append("/").append("profiles").append("/").append("epn.xml").toString());
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
            XMLInputFactory factory = XMLInputFactory.newInstance();
            XMLStreamReader reader = factory.createXMLStreamReader(fileStream);
            while(reader.hasNext()) {
                if (reader.next() == XMLStreamReader.START_ELEMENT) {
                    if ("profile".equals(reader.getLocalName())) {
                        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                            if ("stencilset".equals(reader.getAttributeLocalName(i))) {
                                _stencilSet = reader.getAttributeValue(i);
                            }
                        }
                    } else if ("plugin".equals(reader.getLocalName())) {
                        String name = null;
                        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                            if ("name".equals(reader.getAttributeLocalName(i))) {
                                name = reader.getAttributeValue(i);
                            }
                        }
                        _plugins.put(name, registry.get(name));
                    } else if ("externalloadurl".equals(reader.getLocalName())) {
                        for (int i = 0 ; i < reader.getAttributeCount() ; i++) {
                            if ("name".equals(reader.getAttributeLocalName(i))) {
                                _externalLoadURL = reader.getAttributeValue(i);
                            }
                            if ("usr".equals(reader.getAttributeLocalName(i))) {
                                _usr = reader.getAttributeValue(i);
                            }
                            if ("pwd".equals(reader.getAttributeLocalName(i))) {
                                _pwd = reader.getAttributeValue(i);
                            }
                        }
                    }
                }
            }
        } catch (XMLStreamException e) {
            _logger.error(e.getMessage(), e);
            throw new RuntimeException(e); // stop initialization
        } finally {
            if (fileStream != null) { try { fileStream.close(); } catch(IOException e) {}};
        }
    }
    
    @Override
    public String getName() {
        return "epn";
    }

    @Override
    public String getTitle() {
        return "EPN Designer";
    }

    @Override
    public String getStencilSet() {
        return _stencilSet;
    }

    @Override
    public Collection<String> getStencilSetExtensions() {
        return Collections.emptyList();
    }

    @Override
    public String getSerializedModelExtension() {
        return "epn";
    }

    @Override
    public String getStencilSetURL() {
        return "/designer/stencilsets/epn/epn.json";
    }

    @Override
    public String getStencilSetNamespaceURL() {
        return "http://b3mn.org/stencilset/epn#";
    }

    @Override
    public String getStencilSetExtensionURL() {
        return "http://oryx-editor.org/stencilsets/extensions/epn#";
    }

    @Override
    public Collection<String> getPlugins() {
        return Collections.unmodifiableCollection(_plugins.keySet());
    }

    @Override
    public IDiagramMarshaller createMarshaller() {
        return new IDiagramMarshaller() {
            public String parseModel(String jsonModel) {
                EpnJsonUnmarshaller unmarshaller = new EpnJsonUnmarshaller();
                Object def; //TODO will be replaced with the epn ecore model class (definitions)
                try {
                    def = unmarshaller.unmarshall(jsonModel);
                    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                    //TODO do something now with the model (save it!);
                    return outputStream.toString();
                } catch (JsonParseException e) {
                    _logger.error(e.getMessage(), e);
                } catch (IOException e) {
                    _logger.error(e.getMessage(), e);
                }

                return "";
            }
        };
    }

    @Override
    public IDiagramUnmarshaller createUnmarshaller() {
        return new IDiagramUnmarshaller() {
            public String parseModel(String xmlModel, IDiagramProfile profile) {
                EpnJsonMarshaller marshaller = new EpnJsonMarshaller();
                marshaller.setProfile(profile);
                try {
                    return marshaller.marshall(""); // TODO FIX THIS!
                } catch (Exception e) {
                    _logger.error(e.getMessage(), e);
                }
                return "";
            }
        };
    }

    @Override
    public String getExternalLoadURL() {
        return _externalLoadURL;
    }

    @Override
    public String getUsr() {
        return _usr;
    }

    @Override
    public String getPwd() {
        return _pwd;
    }

}