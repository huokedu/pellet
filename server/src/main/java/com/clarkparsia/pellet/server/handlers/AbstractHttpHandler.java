package com.clarkparsia.pellet.server.handlers;

import java.util.Collection;

import com.clarkparsia.pellet.server.exceptions.ServerException;
import com.clarkparsia.pellet.server.model.OntologyState;
import com.clarkparsia.pellet.server.model.ServerState;
import com.clarkparsia.pellet.service.ServiceDecoder;
import com.clarkparsia.pellet.service.ServiceEncoder;
import com.clarkparsia.pellet.service.reasoner.SchemaReasoner;
import com.google.common.base.Optional;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.reasoner.NodeSet;

/**
 * Abstract implementation of HttpHandler with tools to reuse across all
 * HttpHandlers implemented for the Pellet Server.
 *
 * @author Edgar Rodriguez-Diaz
 */
public abstract class AbstractHttpHandler implements HttpHandler {

	private final ServerState serverState;

	private final Collection<ServiceEncoder> mEncoders;

	private final Collection<ServiceDecoder> mDecoders;

	public AbstractHttpHandler(final ServerState theServerState,
	                           final Collection<ServiceEncoder> theEncoders,
	                           final Collection<ServiceDecoder> theDecoders) {
		serverState = theServerState;
		mEncoders = theEncoders;
		mDecoders = theDecoders;
	}

	protected ServerState getServerState() {
		return serverState;
	}

	protected Optional<ServiceEncoder> getEncoder(final String theMediaType) {
		Optional<ServiceEncoder> aFound = Optional.absent();

		for (ServiceEncoder encoder : mEncoders) {
			if (encoder.canEncode(theMediaType)) {
				aFound = Optional.of(encoder);
			}
		}

		return aFound;
	}

	protected Optional<ServiceDecoder> getDecoder(final String theMediaType) {
		Optional<ServiceDecoder> aFound = Optional.absent();

		for (ServiceDecoder decoder : mDecoders) {
			if (decoder.canDecode(theMediaType)) {
				aFound = Optional.of(decoder);
			}
		}

		return aFound;
	}

	protected SchemaReasoner getReasoner(final IRI theOntology, final String theClientId) throws ServerException {
		Optional<OntologyState> aOntoState = getServerState().getOntology(theOntology);
		if (!aOntoState.isPresent()) {
			throw new ServerException(StatusCodes.NOT_FOUND, "Ontology not found.");
		}
		return aOntoState.get()
		                 .getClient(theClientId)
		                 .getReasoner();
	}

	private String getHeaderValue(final HttpServerExchange theExchange,
	                              final HttpString theAttr,
	                              final String theDefault) {
		HeaderValues aVals = theExchange.getRequestHeaders().get(theAttr);

		return !aVals.isEmpty() ? aVals.getFirst()
		                        : theDefault;
	}

	/**
	 * TODO: Extend to handle multiple Accepts (the encoding/decoding too)
	 */
	protected String getAccept(final HttpServerExchange theExchange) {
		return getHeaderValue(theExchange,
		                      Headers.ACCEPT,
		                      mEncoders.iterator().next().getMediaType());
	}

	protected String getContentType(final HttpServerExchange theExchange) {
		return getHeaderValue(theExchange,
		                      Headers.CONTENT_TYPE,
		                      mDecoders.iterator().next().getMediaType());
	}
}