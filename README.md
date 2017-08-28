# Tree Conditional Entropy

This project is a course requirement for CSE 567: Computational Linguistics at the State University of New York. The aim of the project is to devise an algorithm for calculating the conditional entropy of tree distribution given a PCFG G and a sentence w.

## Introduction
  1. For a given sentence there can be multiple parsed trees possible. Some sentences have a higher probability of occurrence compared to other trees. If there is only one possible sentence construct for a given sentence, we have zero uncertainty about the correct tree, which represents 0 conditional entropy.
  2. Whereas, if there are more than possible trees, the conditional entropy is higher.
  3. In case of completely uniformly distributed trees, the entropy is maximum as we are
equally uncertain about every tree in the distribution.

## Algorithm
We developed a recursive implementation using a chart-based algorithm for calculating conditional entropy of a tree distribution.Similar to inside scores αi,j(P) or outside scores βi,j(P) for non-terminal P spanning i to j, use entropy scores γi,j(P) that store the conditional entropy of the probability distribution over trees that have P as their top label and the words from i to j as their yield:
```
      γi,j(P) = H(t|wi:j , top(t) = P)
```

The entropy of the probability distribution over trees for the whole sentence should be stored
in γ0,n(S), where S is the start symbol and n is the length of the sentence, after running our algorithm. For a case where only single tree is possible, the conditional entropy of the distribution over trees is zero.

This is a bottom-up algorithm similar to inside-outside algorithm.

For validating our algorithm, we shall use approximate probabilities obtained from sampling trees to approximate the entropy of our distribution.

## Recursion

Base case:
```
      γi,i+1(P) = 0
```

For a given tree t, with starting node = P, entropy for a given sentence can be defined as follows:
```
      γi,j(P) = ∑ t:y(t)=wi:,j P(t|wi:j , top(t) = P) * log(1 / P(t|wi:j , top(t) = P))
```

## Explanation
Here is the intuition behind the recursive algorithm we have developed:
  1. Important thing to remember is the additive property of entropy.
  2. While calculating the probability of a parsed tree, we refer to every rule split present in
the example grammar and multiply each probability.
  3. As probability is multiplication of all rule counts in the tree parsing (which we calculated recursively in the inside algorithm), we take logarithm of all the multiplied rules in the entropy calculation (Recall Shannon Entropy formula)
  4. As we know, log (AB) = log A + log B, thus the entropy calculation becomes recursive in addition.
  5. Also, the entropy becomes an addition of entropies of every possible split in a given tree structure, starting from a base non-terminal node to the top node of the tree with S as beginning node.
  6. Since we shall be revisiting the same nodes over and over while traversing different splits, it is wise to cache these calculated values in a chart, which can serve as a look-up table for our dynamic algorithm, saving computational resources and time.
  
## Results
  
  **1. Sentence – “Pat saw the pirates with a telescope”**
  
     P( Pat saw the pirates with a telescope ) = 3.897370366277525E-7 
     
     H( t | Pat saw the pirates with a telescope ) = 0.8995322315533079
     
  **2. Sentence – “Pat saw that the pirates put the roses on the book on a car”**
     
     P( Pat saw that the pirates put the roses on the book on a car ) = 1.352274037969254E-12
     
     H( t | Pat saw that the pirates put the roses on the book on a car ) = 1.8995322315533083
     
## Validating the Algorithm

We use the PCFG sampler to approximate the probabilities of different possible trees for a given sentence. Using these approximated probabilities, we can calculate approximate entropy for each tree, and a summation of these approximated entropies will give us an accurate approximation for the entropy of the distribution over trees given a particular string.

  **1. Given string: Pat saw the pirates with a telescope on a car**
    We experiment with different sample sizes to find out the accurate sample size for getting approximate entropy as close to the exact entropy calculated using the algorithm devised earlier. To establish the best case for comparing the approximate entropy calculated for a given sample size, we shall first calculate the exact entropy using the algorithm described previously.
    
    P( Pat saw the pirates with a telescope on a car ) = 4.706841743964917E-10
    
    H( t | Pat saw the pirates with a telescope on a car )= 2.1284618666768096

Thus the **exact entropy** of the distribution over the trees for given PCFG and the sentence is **2.13.**

Now let us calculate approximate entropy for different sample sizes as shown below:

**1. Samples : 10**

  3 (S (NP Pat) (VP (TVerb saw) (NP (NP (Det the) (Noun pirates) ) (PP (Prep with) (NP (NP (Det a) (Noun telescope) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) ) ) ) )
  
  4 (S (NP Pat) (VP (TVerb saw) (NP (NP (NP (Det the) (Noun pirates)) (PP (Prep with) (NP (Det a) (Noun telescope) ) ) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) ) )
  
  2 (S (NP Pat) (VP (VP (TVerb saw) (NP (Det the) (Noun pirates) ) ) (PP (Prep with) (NP (NP (Det a) (Noun telescope) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) ) ) )
  
  1 (S (NP Pat) (VP (VP (TVerb saw) (NP (NP (Det the) (Noun pirates) ) (PP (Prep with) (NP (Det a) (Noun telescope) ) ) ) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) )

Using Shannon Formula for calculating entropy, we have
Entropy = -(3/10) * log(3/10)/log(2) – (4/10) * log(4/10)/log(2) - (2/10) * log(2/10)/log(2) - (1/10) * log(1/10)/log(2)
        = 1.8464393445

Therefore, for sample size of 10, **H = 1.85**

**2. Sample size : 100**

  29 (S (NP Pat) (VP (TVerb saw) (NP (NP (Det the) (Noun pirates) ) (PP (Prep with) (NP (NP (Det a) (Noun telescope) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) ) ) ) )

  36 (S (NP Pat) (VP (TVerb saw) (NP (NP (NP (Det the) (Noun pirates) ) (PP (Prep with) (NP (Det a) (Noun telescope) ) ) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) ) )
  
  14 (S (NP Pat) (VP (VP (TVerb saw) (NP (Det the) (Noun pirates) ) ) (PP (Prep with) (NP (NP (Det a) (Noun telescope) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) ) ) )
  
  16 (S (NP Pat) (VP (VP (TVerb saw) (NP (NP (Det the) (Noun pirates) ) (PP (Prep with) (NP (Det a) (Noun telescope) ) ) ) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) )
  
  5 (S (NP Pat) (VP (VP (VP (TVerb saw) (NP (Det the) (Noun pirates) ) ) (PP (Prep with) (NP (Det a) (Noun telescope) ) ) ) (PP (Prep on) (NP (Det a) (Noun car) ) ) ) )
  
Entropy = -(29/100) * log(29/100)/log(2) - (36/100) * log(36/100)/log(2) - (14/100) * log(14/100)/log(2) - (16/100) * log(16/100)/log(2) - (5/100) * log(5/100)/log(2)
        = 2.0847426066
        
Therefore, for sample size of 100, **H = 2.08**

Similarly **we calculate for sample sizes 500, 750, 1000** respectively. The values for approximate entropy are as depicted below:
  
  for sample size of 500, H = 2.10
  
  for sample size of 750, H = 2.09
  
  for sample size of 1000, H = 2.09

## Conclusion
**As we can see, with enough samples as in case of 500, 750 and 1000, the value of approximate entropy is very close to the actual value.**

The reason is that, when the number of samples is too low, the rare and less probable tree structures for a sentence might not have any representation to contribute to entropy, but with more samples, they can express the ambiguity in a better way.

## Credits
I would like to thank Prof. John Pate for teaching this course and guiding me through this project.

