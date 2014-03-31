package com.lokivog.mws.products;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.amazonservices.mws.products.MarketplaceWebServiceProducts;
import com.amazonservices.mws.products.MarketplaceWebServiceProductsClient;
import com.amazonservices.mws.products.MarketplaceWebServiceProductsException;
import com.amazonservices.mws.products.mock.MarketplaceWebServiceProductsMock;
import com.amazonservices.mws.products.model.ASINIdentifier;
import com.amazonservices.mws.products.model.AttributeSetList;
import com.amazonservices.mws.products.model.CompetitivePriceList;
import com.amazonservices.mws.products.model.CompetitivePriceType;
import com.amazonservices.mws.products.model.CompetitivePricingType;
import com.amazonservices.mws.products.model.GetMatchingProductForIdRequest;
import com.amazonservices.mws.products.model.GetMatchingProductForIdResponse;
import com.amazonservices.mws.products.model.GetMatchingProductForIdResult;
import com.amazonservices.mws.products.model.IdListType;
import com.amazonservices.mws.products.model.IdentifierType;
import com.amazonservices.mws.products.model.LowestOfferListingList;
import com.amazonservices.mws.products.model.LowestOfferListingType;
import com.amazonservices.mws.products.model.MoneyType;
import com.amazonservices.mws.products.model.NumberOfOfferListingsList;
import com.amazonservices.mws.products.model.OfferListingCountType;
import com.amazonservices.mws.products.model.OfferType;
import com.amazonservices.mws.products.model.OffersList;
import com.amazonservices.mws.products.model.PriceType;
import com.amazonservices.mws.products.model.Product;
import com.amazonservices.mws.products.model.ProductList;
import com.amazonservices.mws.products.model.ProductsUtil;
import com.amazonservices.mws.products.model.QualifiersType;
import com.amazonservices.mws.products.model.RelationshipList;
import com.amazonservices.mws.products.model.ResponseMetadata;
import com.amazonservices.mws.products.model.SalesRankList;
import com.amazonservices.mws.products.model.SalesRankType;
import com.amazonservices.mws.products.model.SellerSKUIdentifier;
import com.amazonservices.mws.products.model.ShippingTimeType;
import com.amazonservices.mws.products.samples.ProductsConfig;

//IMPORTANT: The Amazon MWS Java Client API is required to run this class. Download from the Amazon developer website. Once downloaded
//add the client library to the classpath
/**
 * The Class GetMatchingProductForId. Calls the Amazon MWS Product Feed API to retrieve a list of products matching a list of UPC ids.
 */
public class GetMatchingProductForId {

	final Logger logger = LoggerFactory.getLogger(GetMatchingProductForId.class);

	private List<String> mProductIds;

	public GetMatchingProductForId(String pId) {

	}

	public GetMatchingProductForId(List<String> pProductIds) {
		mProductIds = pProductIds;
	}

	public GetMatchingProductForIdResponse matchProducts() {
		MarketplaceWebServiceProducts service;
		boolean mock = false;
		if (mock) {
			service = new MarketplaceWebServiceProductsMock();
		} else {
			service = new MarketplaceWebServiceProductsClient(ProductsConfig.accessKeyId,
					ProductsConfig.secretAccessKey, ProductsConfig.applicationName, ProductsConfig.applicationVersion,
					ProductsConfig.config);
		}

		/************************************************************************
		 * Setup request parameters and uncomment invoke to try out sample for Get Matching Product For Id
		 ***********************************************************************/
		GetMatchingProductForIdRequest request = new GetMatchingProductForIdRequest();
		request.setSellerId(ProductsConfig.sellerId);
		request.setMarketplaceId(ProductsConfig.marketplaceId);
		IdListType idListType = new IdListType();
		idListType.setId(mProductIds);
		request.setIdList(idListType);
		request.setIdType("UPC");
		GetMatchingProductForIdResponse response = null;
		try {
			response = invokeGetMatchingProductForId(service, request);
			generateElasticSearchJson(response);
		} catch (Exception e) {
			logger.error("exception connection to elasticsearch", e);
		}
		return response;
	}

	/**
	 * Just add few required parameters, and try the service Get Matching Product For Id functionality
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String... args) {
		List<String> ids = new ArrayList<String>();
		ids.add("731015140640");
		ids.add("633040306001");
		GetMatchingProductForId matchingProductForId = new GetMatchingProductForId(ids);
		matchingProductForId.test();
	}

	public void test() {
		GetMatchingProductForIdResponse response = matchProducts();
		List<GetMatchingProductForIdResult> matchingProductForIdResultList = response
				.getGetMatchingProductForIdResult();
		logger.info(response.toJSON());
		// JSONObject object = new JSONObject(response.toJSON());
		// JSONObject jsonResponse = object.getJSONObject("GetMatchingProductForIdResponse");
		// JSONArray jsonResult = jsonResponse.getJSONArray("GetMatchingProductForIdResult");
		// JSONObject products = jsonResult.getJSONObject(0);
	}

	public void generateElasticSearchJson(GetMatchingProductForIdResponse pResponse) {
		List<GetMatchingProductForIdResult> matchingProductForIdResultList = pResponse
				.getGetMatchingProductForIdResult();
		StringBuilder builder = new StringBuilder();
		for (GetMatchingProductForIdResult getMatchingProductForIdResult : matchingProductForIdResultList) {
			ProductList products = getMatchingProductForIdResult.getProducts();
			java.util.List<Product> productList = products.getProduct();
			for (Product product : productList) {
				builder.append("MarketplaceASIN = " + product.getIdentifiers().getMarketplaceASIN());
			}
		}
		logger.debug("builder=" + builder.toString());
	}

	/**
	 * Get Matching Product For Id request sample GetMatchingProduct will return the details (attributes) for the given Identifier list.
	 * Identifer type can be one of [SKU|ASIN|UPC|EAN|ISBN|GTIN|JAN]
	 * 
	 * @param service
	 *            instance of MarketplaceWebServiceProducts service
	 * @param request
	 *            Action to invoke
	 */
	public GetMatchingProductForIdResponse invokeGetMatchingProductForId(MarketplaceWebServiceProducts service,
			GetMatchingProductForIdRequest request) {
		GetMatchingProductForIdResponse response = null;
		PrintWriter printWriter = null;
		try {
			printWriter = new PrintWriter(new File("output/matchingProductForId"));
			response = service.getMatchingProductForId(request);
			logger.debug("GetMatchingProductForId Action Response");
			List<GetMatchingProductForIdResult> getMatchingProductForIdResultList = response
					.getGetMatchingProductForIdResult();
			JSONObject object = new JSONObject();
			// Iterates over the Amazon response and builds a json object from the xml response.
			// This doesn't set all the properties from the xml schema object but it does contain place holders to set them in the future
			// if needed
			for (GetMatchingProductForIdResult getMatchingProductForIdResult : getMatchingProductForIdResultList) {
				if (getMatchingProductForIdResult.isSetId()) {
					object.put("id", getMatchingProductForIdResult.getId());
				}
				if (getMatchingProductForIdResult.isSetIdType()) {
					object.put("idType", getMatchingProductForIdResult.getIdType());
				}
				if (getMatchingProductForIdResult.isSetStatus()) {
					object.put("status", getMatchingProductForIdResult.getStatus());
				}
				if (getMatchingProductForIdResult.isSetProducts()) {
					ProductList products = getMatchingProductForIdResult.getProducts();
					JSONArray array = new JSONArray();
					java.util.List<Product> productList = products.getProduct();
					for (Product product : productList) {
						JSONObject jsonProduct = new JSONObject();
						if (product.isSetIdentifiers()) {
							IdentifierType identifiers = product.getIdentifiers();
							if (identifiers.isSetMarketplaceASIN()) {
								ASINIdentifier marketplaceASIN = identifiers.getMarketplaceASIN();
								if (marketplaceASIN.isSetMarketplaceId()) {
									jsonProduct.put("marketplaceId", marketplaceASIN.getMarketplaceId());
								}
								if (marketplaceASIN.isSetASIN()) {
									jsonProduct.put("asin", marketplaceASIN.getASIN());
								}
							}
							if (identifiers.isSetSKUIdentifier()) {
								SellerSKUIdentifier SKUIdentifier = identifiers.getSKUIdentifier();
								if (SKUIdentifier.isSetMarketplaceId()) {
								}
								if (SKUIdentifier.isSetSellerId()) {
									jsonProduct.put("sellerId", SKUIdentifier.getSellerId());
								}
								if (SKUIdentifier.isSetSellerSKU()) {
									jsonProduct.put("sellerSKU", SKUIdentifier.getSellerSKU());
								}
							}
						}
						if (product.isSetAttributeSets()) {
							AttributeSetList attributeSetList = product.getAttributeSets();
							for (Object obj : attributeSetList.getAny()) {
								Node attribute = (Node) obj;
								NodeList nodeList = attribute.getChildNodes();
								for (int i = 0; i < nodeList.getLength(); i++) {
									String nodeName = nodeList.item(i).getNodeName();
									nodeName = nodeName.replaceFirst("ns2:", "");
									jsonProduct.put(nodeName, nodeList.item(i).getTextContent());

								}
								NamedNodeMap attributes = attribute.getAttributes();
							}
						}
						if (product.isSetRelationships()) {
							RelationshipList relationships = product.getRelationships();
							for (Object obj : relationships.getAny()) {
								Node relationship = (Node) obj;
								System.out.println(ProductsUtil.formatXml(relationship));
							}
						}
						if (product.isSetCompetitivePricing()) {
							CompetitivePricingType competitivePricing = product.getCompetitivePricing();
							if (competitivePricing.isSetCompetitivePrices()) {
								CompetitivePriceList competitivePrices = competitivePricing.getCompetitivePrices();
								java.util.List<CompetitivePriceType> competitivePriceList = competitivePrices
										.getCompetitivePrice();
								for (CompetitivePriceType competitivePrice : competitivePriceList) {
									if (competitivePrice.isSetCondition()) {
									}
									if (competitivePrice.isSetSubcondition()) {
									}
									if (competitivePrice.isSetBelongsToRequester()) {
									}
									if (competitivePrice.isSetCompetitivePriceId()) {
									}
									if (competitivePrice.isSetPrice()) {
										PriceType price = competitivePrice.getPrice();
										if (price.isSetLandedPrice()) {
											MoneyType landedPrice = price.getLandedPrice();
											if (landedPrice.isSetCurrencyCode()) {
											}
											if (landedPrice.isSetAmount()) {
											}
										}
										if (price.isSetListingPrice()) {
											MoneyType listingPrice = price.getListingPrice();
											if (listingPrice.isSetCurrencyCode()) {
											}
											if (listingPrice.isSetAmount()) {
											}
										}
										if (price.isSetShipping()) {
											MoneyType shipping = price.getShipping();
											if (shipping.isSetCurrencyCode()) {
											}
											if (shipping.isSetAmount()) {

											}
										}
									}
								}
							}
							if (competitivePricing.isSetNumberOfOfferListings()) {
								NumberOfOfferListingsList numberOfOfferListings = competitivePricing
										.getNumberOfOfferListings();
								java.util.List<OfferListingCountType> offerListingCountList = numberOfOfferListings
										.getOfferListingCount();
								for (OfferListingCountType offerListingCount : offerListingCountList) {
									if (offerListingCount.isSetCondition()) {
									}
									if (offerListingCount.isSetValue()) {
									}
								}
							}
							if (competitivePricing.isSetTradeInValue()) {
								MoneyType tradeInValue = competitivePricing.getTradeInValue();
								if (tradeInValue.isSetCurrencyCode()) {
								}
								if (tradeInValue.isSetAmount()) {
								}
							}
						}
						if (product.isSetSalesRankings()) {
							SalesRankList salesRankings = product.getSalesRankings();
							java.util.List<SalesRankType> salesRankList = salesRankings.getSalesRank();
							for (SalesRankType salesRank : salesRankList) {
								if (salesRank.isSetProductCategoryId()) {
								}
								if (salesRank.isSetRank()) {
								}
							}
						}
						if (product.isSetLowestOfferListings()) {
							LowestOfferListingList lowestOfferListings = product.getLowestOfferListings();
							java.util.List<LowestOfferListingType> lowestOfferListingList = lowestOfferListings
									.getLowestOfferListing();
							for (LowestOfferListingType lowestOfferListing : lowestOfferListingList) {
								if (lowestOfferListing.isSetQualifiers()) {
									QualifiersType qualifiers = lowestOfferListing.getQualifiers();
									if (qualifiers.isSetItemCondition()) {
									}
									if (qualifiers.isSetItemSubcondition()) {
									}
									if (qualifiers.isSetFulfillmentChannel()) {
									}
									if (qualifiers.isSetShipsDomestically()) {
									}
									if (qualifiers.isSetShippingTime()) {
										ShippingTimeType shippingTime = qualifiers.getShippingTime();
										if (shippingTime.isSetMax()) {
										}
									}
									if (qualifiers.isSetSellerPositiveFeedbackRating()) {
									}
								}
								if (lowestOfferListing.isSetNumberOfOfferListingsConsidered()) {
								}
								if (lowestOfferListing.isSetSellerFeedbackCount()) {
								}
								if (lowestOfferListing.isSetPrice()) {
									PriceType price1 = lowestOfferListing.getPrice();
									if (price1.isSetLandedPrice()) {
										MoneyType landedPrice1 = price1.getLandedPrice();
										if (landedPrice1.isSetCurrencyCode()) {
										}
										if (landedPrice1.isSetAmount()) {
										}
									}
									if (price1.isSetListingPrice()) {
										MoneyType listingPrice1 = price1.getListingPrice();
										if (listingPrice1.isSetCurrencyCode()) {
										}
										if (listingPrice1.isSetAmount()) {
										}
									}
									if (price1.isSetShipping()) {
										MoneyType shipping1 = price1.getShipping();
										if (shipping1.isSetCurrencyCode()) {
										}
										if (shipping1.isSetAmount()) {
										}
									}
								}
								if (lowestOfferListing.isSetMultipleOffersAtLowestPrice()) {
								}
							}
						}
						if (product.isSetOffers()) {
							OffersList offers = product.getOffers();
							java.util.List<OfferType> offerList = offers.getOffer();
							for (OfferType offer : offerList) {
								if (offer.isSetBuyingPrice()) {
									PriceType buyingPrice = offer.getBuyingPrice();
									if (buyingPrice.isSetLandedPrice()) {
										MoneyType landedPrice2 = buyingPrice.getLandedPrice();
										if (landedPrice2.isSetCurrencyCode()) {
										}
										if (landedPrice2.isSetAmount()) {
										}
									}
									if (buyingPrice.isSetListingPrice()) {
										MoneyType listingPrice2 = buyingPrice.getListingPrice();
										if (listingPrice2.isSetCurrencyCode()) {
										}
										if (listingPrice2.isSetAmount()) {
										}
									}
									if (buyingPrice.isSetShipping()) {
										MoneyType shipping2 = buyingPrice.getShipping();
										if (shipping2.isSetCurrencyCode()) {
										}
										if (shipping2.isSetAmount()) {
										}
									}
								}
								if (offer.isSetRegularPrice()) {
									MoneyType regularPrice = offer.getRegularPrice();
									if (regularPrice.isSetCurrencyCode()) {
									}
									if (regularPrice.isSetAmount()) {
									}
								}
								if (offer.isSetFulfillmentChannel()) {
								}
								if (offer.isSetItemCondition()) {
								}
								if (offer.isSetItemSubCondition()) {
								}
								if (offer.isSetSellerId()) {
								}
								if (offer.isSetSellerSKU()) {
								}
							}
						}
						array.put(jsonProduct);
					}
					object.put("products", array);
					printWriter.println(object.toString());
					logger.info("jsonProduct: " + object);
				}
				if (getMatchingProductForIdResult.isSetError()) {
					com.amazonservices.mws.products.model.Error error = getMatchingProductForIdResult.getError();
					logger.warn("Amazon Response Error - Type: {}, Code: {}, Message: {}", error.getType(),
							error.getCode(), error.getMessage());
				}
			}
			if (response.isSetResponseMetadata()) {
				ResponseMetadata responseMetadata = response.getResponseMetadata();
				if (responseMetadata.isSetRequestId()) {
					logger.debug("responseMetadata requestId {}", responseMetadata.getRequestId());
				}
			}

		} catch (MarketplaceWebServiceProductsException ex) {
			logger.error("Caught Exception: " + ex.getMessage());
			logger.error("Response Status Code: " + ex.getStatusCode());
			logger.error("Error Code: " + ex.getErrorCode());
			logger.error("Error Type: " + ex.getErrorType());
			logger.error("Request ID: " + ex.getRequestId());
			logger.error("XML: " + ex.getXML());
			logger.error("ResponseHeaderMetadata: " + ex.getResponseHeaderMetadata());
		} catch (FileNotFoundException e) {
			logger.error("FileNotFound", e);
		} finally {
			if (printWriter != null) {
				printWriter.close();
			}
		}
		return response;
	}
}
