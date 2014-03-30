package com.lokivog.mws.products;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;
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

public class GetMatchingProductForId {

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
		 * Setup request parameters and uncomment invoke to try out sample for
		 * Get Matching Product For Id
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
			// response = service.getMatchingProductForId(request);
			response = invokeGetMatchingProductForId(service, request);
			generateElasticSearchJson(response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return response;
	}

	/**
	 * Just add few required parameters, and try the service Get Matching
	 * Product For Id functionality
	 * 
	 * @param args
	 *            unused
	 */
	public static void main(String... args) {
		List<String> ids = new ArrayList<String>();
		ids.add("731015140640");
		ids.add("633040306001");
		GetMatchingProductForId matchingProductForId = new GetMatchingProductForId(ids);
		GetMatchingProductForIdResponse response = matchingProductForId.matchProducts();
		List<GetMatchingProductForIdResult> matchingProductForIdResultList = response
				.getGetMatchingProductForIdResult();

		System.out.println(response.toXML());
		// System.out.println(response.toJSON());
		JSONObject object = new JSONObject(response.toJSON());
		JSONObject jsonResponse = object.getJSONObject("GetMatchingProductForIdResponse");
		// System.out.println("jsonResponse: " + jsonResponse.toString());
		JSONArray jsonResult = jsonResponse.getJSONArray("GetMatchingProductForIdResult");
		// System.out.println("jsonResult: " + jsonResult.toString());
		JSONObject products = jsonResult.getJSONObject(0);
		// System.out.println(products.toString());
		// for (GetMatchingProductForIdResult getMatchingProductForIdResult :
		// matchingProductForIdResultList) {
		// System.out.println(getMatchingProductForIdResult.toString());
		// System.out.println(getMatchingProductForIdResult.toXMLFragment());
		// }

	}

	public void generateElasticSearchJson(GetMatchingProductForIdResponse pResponse) {
		List<GetMatchingProductForIdResult> matchingProductForIdResultList = pResponse
				.getGetMatchingProductForIdResult();
		StringBuilder builder = new StringBuilder();
		for (GetMatchingProductForIdResult getMatchingProductForIdResult : matchingProductForIdResultList) {
			// getMatchingProductForIdResult.g
			// System.out.println("ID=" +
			// getMatchingProductForIdResult.getIdType());
			ProductList products = getMatchingProductForIdResult.getProducts();
			java.util.List<Product> productList = products.getProduct();
			for (Product product : productList) {

				builder.append("MarketplaceASIN = " + product.getIdentifiers().getMarketplaceASIN());
			}
			// System.out.println(getMatchingProductForIdResult.toXMLFragment());
			// }
		}
		System.out.println("builder=" + builder.toString());
	}

	/**
	 * Get Matching Product For Id request sample GetMatchingProduct will return
	 * the details (attributes) for the given Identifier list. Identifer type
	 * can be one of [SKU|ASIN|UPC|EAN|ISBN|GTIN|JAN]
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

			System.out.println("GetMatchingProductForId Action Response");
			System.out.println("=============================================================================");
			System.out.println();

			System.out.println("    GetMatchingProductForIdResponse");
			System.out.println();
			List<GetMatchingProductForIdResult> getMatchingProductForIdResultList = response
					.getGetMatchingProductForIdResult();
			JSONObject object = new JSONObject();
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
								System.out.println(ProductsUtil.formatXml(attribute));

							}
							System.out.println();
						}
						if (product.isSetRelationships()) {
							// System.out.println("                    Relationships");
							RelationshipList relationships = product.getRelationships();
							for (Object obj : relationships.getAny()) {
								Node relationship = (Node) obj;
								System.out.println(ProductsUtil.formatXml(relationship));
							}
							// System.out.println();
						}
						if (product.isSetCompetitivePricing()) {
							// System.out.println("                    CompetitivePricing");
							CompetitivePricingType competitivePricing = product.getCompetitivePricing();
							if (competitivePricing.isSetCompetitivePrices()) {
								// System.out.println("                        CompetitivePrices");
								CompetitivePriceList competitivePrices = competitivePricing.getCompetitivePrices();
								java.util.List<CompetitivePriceType> competitivePriceList = competitivePrices
										.getCompetitivePrice();
								for (CompetitivePriceType competitivePrice : competitivePriceList) {
									// System.out.println("                            CompetitivePrice");
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
										System.out.println("                                ListingPrice");
										System.out.println();
										MoneyType listingPrice1 = price1.getListingPrice();
										if (listingPrice1.isSetCurrencyCode()) {
											System.out.println("                                    CurrencyCode");
											System.out.println();
											System.out.println("                                        "
													+ listingPrice1.getCurrencyCode());
											System.out.println();
										}
										if (listingPrice1.isSetAmount()) {
											System.out.println("                                    Amount");
											System.out.println();
											System.out.println("                                        "
													+ listingPrice1.getAmount());
											System.out.println();
										}
									}
									if (price1.isSetShipping()) {
										System.out.println("                                Shipping");
										System.out.println();
										MoneyType shipping1 = price1.getShipping();
										if (shipping1.isSetCurrencyCode()) {
											System.out.println("                                    CurrencyCode");
											System.out.println();
											System.out.println("                                        "
													+ shipping1.getCurrencyCode());
											System.out.println();
										}
										if (shipping1.isSetAmount()) {
											System.out.println("                                    Amount");
											System.out.println();
											System.out.println("                                        "
													+ shipping1.getAmount());
											System.out.println();
										}
									}
								}
								if (lowestOfferListing.isSetMultipleOffersAtLowestPrice()) {
									System.out.println("                            MultipleOffersAtLowestPrice");
									System.out.println();
									System.out.println("                                "
											+ lowestOfferListing.getMultipleOffersAtLowestPrice());
									System.out.println();
								}
							}
						}
						if (product.isSetOffers()) {
							System.out.println("                    Offers");
							System.out.println();
							OffersList offers = product.getOffers();
							java.util.List<OfferType> offerList = offers.getOffer();
							for (OfferType offer : offerList) {
								System.out.println("                        Offer");
								System.out.println();
								if (offer.isSetBuyingPrice()) {
									System.out.println("                            BuyingPrice");
									System.out.println();
									PriceType buyingPrice = offer.getBuyingPrice();
									if (buyingPrice.isSetLandedPrice()) {
										System.out.println("                                LandedPrice");
										System.out.println();
										MoneyType landedPrice2 = buyingPrice.getLandedPrice();
										if (landedPrice2.isSetCurrencyCode()) {
											System.out.println("                                    CurrencyCode");
											System.out.println();
											System.out.println("                                        "
													+ landedPrice2.getCurrencyCode());
											System.out.println();
										}
										if (landedPrice2.isSetAmount()) {
											System.out.println("                                    Amount");
											System.out.println();
											System.out.println("                                        "
													+ landedPrice2.getAmount());
											System.out.println();
										}
									}
									if (buyingPrice.isSetListingPrice()) {
										System.out.println("                                ListingPrice");
										System.out.println();
										MoneyType listingPrice2 = buyingPrice.getListingPrice();
										if (listingPrice2.isSetCurrencyCode()) {
											System.out.println("                                    CurrencyCode");
											System.out.println();
											System.out.println("                                        "
													+ listingPrice2.getCurrencyCode());
											System.out.println();
										}
										if (listingPrice2.isSetAmount()) {
											System.out.println("                                    Amount");
											System.out.println();
											System.out.println("                                        "
													+ listingPrice2.getAmount());
											System.out.println();
										}
									}
									if (buyingPrice.isSetShipping()) {
										System.out.println("                                Shipping");
										System.out.println();
										MoneyType shipping2 = buyingPrice.getShipping();
										if (shipping2.isSetCurrencyCode()) {
											System.out.println("                                    CurrencyCode");
											System.out.println();
											System.out.println("                                        "
													+ shipping2.getCurrencyCode());
											System.out.println();
										}
										if (shipping2.isSetAmount()) {
											System.out.println("                                    Amount");
											System.out.println();
											System.out.println("                                        "
													+ shipping2.getAmount());
											System.out.println();
										}
									}
								}
								if (offer.isSetRegularPrice()) {
									System.out.println("                            RegularPrice");
									System.out.println();
									MoneyType regularPrice = offer.getRegularPrice();
									if (regularPrice.isSetCurrencyCode()) {
										System.out.println("                                CurrencyCode");
										System.out.println();
										System.out.println("                                    "
												+ regularPrice.getCurrencyCode());
										System.out.println();
									}
									if (regularPrice.isSetAmount()) {
										System.out.println("                                Amount");
										System.out.println();
										System.out.println("                                    "
												+ regularPrice.getAmount());
										System.out.println();
									}
								}
								if (offer.isSetFulfillmentChannel()) {
									System.out.println("                            FulfillmentChannel");
									System.out.println();
									System.out.println("                                "
											+ offer.getFulfillmentChannel());
									System.out.println();
								}
								if (offer.isSetItemCondition()) {
									System.out.println("                            ItemCondition");
									System.out.println();
									System.out.println("                                " + offer.getItemCondition());
									System.out.println();
								}
								if (offer.isSetItemSubCondition()) {
									System.out.println("                            ItemSubCondition");
									System.out.println();
									System.out
											.println("                                " + offer.getItemSubCondition());
									System.out.println();
								}
								if (offer.isSetSellerId()) {
									System.out.println("                            SellerId");
									System.out.println();
									System.out.println("                                " + offer.getSellerId());
									System.out.println();
								}
								if (offer.isSetSellerSKU()) {
									System.out.println("                            SellerSKU");
									System.out.println();
									System.out.println("                                " + offer.getSellerSKU());
									System.out.println();
								}
							}
						}
						array.put(jsonProduct);
					}
					object.put("products", array);
					printWriter.println(object.toString());
					System.out.println("jsonProduct: " + object);
				}
				if (getMatchingProductForIdResult.isSetError()) {
					System.out.println("            Error");
					System.out.println();
					com.amazonservices.mws.products.model.Error error = getMatchingProductForIdResult.getError();
					if (error.isSetType()) {
						System.out.println("                Type");
						System.out.println();
						System.out.println("                    " + error.getType());
						System.out.println();
					}
					if (error.isSetCode()) {
						System.out.println("                Code");
						System.out.println();
						System.out.println("                    " + error.getCode());
						System.out.println();
					}
					if (error.isSetMessage()) {
						System.out.println("                Message");
						System.out.println();
						System.out.println("                    " + error.getMessage());
						System.out.println();
					}
				}
			}
			if (response.isSetResponseMetadata()) {
				System.out.println("        ResponseMetadata");
				System.out.println();
				ResponseMetadata responseMetadata = response.getResponseMetadata();
				if (responseMetadata.isSetRequestId()) {
					System.out.println("            RequestId");
					System.out.println();
					System.out.println("                " + responseMetadata.getRequestId());
					System.out.println();
				}
			}
			System.out.println();
			System.out.println(response.getResponseHeaderMetadata());
			System.out.println();

		} catch (MarketplaceWebServiceProductsException ex) {

			System.out.println("Caught Exception: " + ex.getMessage());
			System.out.println("Response Status Code: " + ex.getStatusCode());
			System.out.println("Error Code: " + ex.getErrorCode());
			System.out.println("Error Type: " + ex.getErrorType());
			System.out.println("Request ID: " + ex.getRequestId());
			System.out.println("XML: " + ex.getXML());
			System.out.print("ResponseHeaderMetadata: " + ex.getResponseHeaderMetadata());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (printWriter != null) {
				printWriter.close();
			}
		}
		return response;
	}

}
