Java Heap Analyzer
===================
This is a Java (byetcode) analyzer based on the Chord Platform

It is based on the Theory of Abstract Interpretation, and performs sharing and
cyclicity analysis by using an abstract domain which tracks the fields involved
in sharing and cyclicity paths in the heap.

For example, consider a standard double-linked list implemented as a List class
with next and prev fields, and whose length is at least 2.  If two variables x
and y point to the beginning and the end of a list, respectively, then
traditional sharing and cyclicity analyses detect that

- x and y share, i.e., there are two paths in the heap, one accessible from x
  and the other accessible from y, which end in the same memory location (in
  this case, any element in the list is reachable from both variables)
- x and y are both cyclic, i.e., there are paths in the heap, starting from 
  any of the two variables, which reach the same location more than once (in
  this case, the data structure allow to go back to the same location by 
  accessing the next field and, afterwards, accessing the prev field)
  
On the other hand, the implemented analysis gives more information in that

- it detects that any two paths Px and Py starting from x and y, 
  respectively, and reaching a common location,
  - Px certainly traverses the "next" field
  - Py certainly traverses the "prev" field 
- as for cyclicity, any cycle will certainly involve both the "next" and the
  "prev" field
  
The latter result allows termination analysis to prove that a standard
traversal of the list (e.g., "x = x.next" until "x==null") will terminate
IN SPITE OF the data structure being cyclic.

#### References:

- Damiano Zanardini and Samir Genaim. Inference of Field-Sensitive Reachability and Cyclicity. ACM Transactions on Computational Logic, 15(4/33), pages 1-41, 2014. ACM Press, New York.

- Damiano Zanardini. Field-Sensitive Sharing.
(hopefully, to be published! Please contact me if you want more details)

