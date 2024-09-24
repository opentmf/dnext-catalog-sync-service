package com.pia.catalog.sync.model;

/**
 * @author Gokhan Demir
 */
public enum EntityType {
  /**
   * <ol>
   *   <li><strong>For version = "0"</strong></li>
   * <ul>
   *   <li>version: "0"</li>
   *   <li>validFor: start date: old, end date: today</li>
   *   <li>lifecycleStatus: "In design"</li>
   * </ul>
   * <li><strong>For version = "1"</strong></li>
   * <ul>
   *   <li>version: "1"</li>
   *   <li>validFor: startDate: today, end date: null</li>
   *   <li>lifecycleStatus: "Launched"</li>
   * </ul>
   * </ol>
   *
   */
  MULTI_VERSIONED,

  /**
   * <ul>
   *   <li>version: "1"</li>
   *   <li>validFor: startDate: today, end date: null</li>
   *   <li>lifecycleStatus: "Launched"</li>
   * </ul>
   */
  SINGLE_VERSIONED,

  /**
   * Create entity as-is according to the supplied payload.
   */
  NORMAL
}
